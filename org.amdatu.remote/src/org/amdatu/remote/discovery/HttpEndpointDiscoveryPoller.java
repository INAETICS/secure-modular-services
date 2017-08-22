/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.amdatu.remote.discovery;

import static org.amdatu.remote.EndpointUtil.readEndpoints;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.amdatu.remote.IOUtil;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * The EndpointDiscoveryPoller provides generic polling for multiple discovery endpoints as provided
 * by {@link HttpEndpointDiscoveryServlet} that may be added and removed over time.
 * 
 * Calls {@link AbstractDiscovery#addDiscoveredEndpoint(EndpointDescription) 
 * and {@link AbstractDiscovery#removeDiscoveredEndpoint(EndpointDescription) as required.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * 
 */
public final class HttpEndpointDiscoveryPoller {

    private final ConcurrentHashMap<String, EndpointHolder> m_endpoints = new ConcurrentHashMap<String, EndpointHolder>();
    private final AbstractDiscovery m_discovery;
    private final HttpEndpointDiscoveryConfiguration m_configuration;

    private volatile ScheduledFuture<?> m_future;

    public HttpEndpointDiscoveryPoller(ScheduledExecutorService executor, AbstractDiscovery discovery,
        HttpEndpointDiscoveryConfiguration configuration) {
        m_discovery = discovery;
        m_configuration = configuration;
        m_future = executor.scheduleAtFixedRate(new UpdateRunnable(), 1, configuration.getSchedule(), TimeUnit.SECONDS);
    }

    public void cancel() {
        m_future.cancel(false);
        m_endpoints.clear();
    }

    /**
     * Add a newly discovered HTTP endpoint.
     * 
     * @param id a unique identifier
     * @param url The HTTP endpoint
     */
    public synchronized void addDiscoveryEndpoint(String id, URL url) {
        m_discovery.logDebug("Adding discovery endpoint, id: %s, url: %s", id, url);

        // AMDATURS-128
        // When a remote endpoint crashes, it can happen that its endpoint holder is still registered
        // when the restarted endpoint is discovered.
        // In order to prevent that the old endpointholder unregisters services of this new endpoint when
        // it finally expires, search for it based on the url and close it.
        // Also close old endpoints with the same id, but different url.
        for (Entry<String, EndpointHolder> endpointEntry : m_endpoints.entrySet()) {
            String oldId = endpointEntry.getKey();
            URL oldUrl = endpointEntry.getValue().getEndpointUrl();
            if (oldUrl.equals(url) || (oldId.equals(id) && !oldUrl.equals(url))) {
                m_discovery.logDebug("Closing old endpoint, id: %s, url %s)", oldId, oldUrl);
                removeDiscoveryEndpoint(oldId);
            }
        }

        m_endpoints.putIfAbsent(id, new EndpointHolder(url));
    }

    /**
     * Set a collection of Ids / URLs
     * 
     * @param newEndpoints a map of <String, URL>, with the String being a unique id
     */
    public synchronized void setDiscoveryEndpoints(Map<String, URL> newEndpoints) {
        // first remove old urls
        Set<String> idsToRemove = new HashSet<>();
        Set<String> newIds = newEndpoints.keySet();
        for (String oldId : m_endpoints.keySet()) {
            if (!newIds.contains(oldId)) {
                idsToRemove.add(oldId);
            }
        }
        for (String removedId : idsToRemove) {
            removeDiscoveryEndpoint(removedId);
        }
        // add missing urls
        for (Map.Entry<String, URL> newEndpoint : newEndpoints.entrySet()) {
            if (!m_endpoints.containsKey(newEndpoint.getKey())) {
                addDiscoveryEndpoint(newEndpoint.getKey(), newEndpoint.getValue());
            }
        }
    }

    /**
     * Removed a previously discovered HTTP endpoint.
     * 
     * @param id The HTTP endpoint id
     */
    public synchronized void removeDiscoveryEndpoint(String id) {
        m_discovery.logDebug("Removing discovery endpoint id: %s", id);
        EndpointHolder holder = m_endpoints.remove(id);
        if (holder != null) {
            holder.close();
        }
    }

    /*
     * Runnable that is scheduled to sequentially run update on all registered
     * Discovery Endpoints at a fixed rate.
     */
    private final class UpdateRunnable implements Runnable {

        @Override
        public void run() {
            m_discovery.logDebug("Start updating discovery endpoints");
            for (EndpointHolder holder : m_endpoints.values()) {
                try {
                    holder.update();
                }
                catch (Exception e) {
                    // Do not kill the update task. Holder ensures this never happens
                }
            }
            m_discovery.logDebug("Done updating discovery endpoints");
        }
    }

    /*
     * Holder that keeps state and update logic for a registered Discovery
     * Endpoint.
     * 
     * note: #update() and #stop() are simply synchronized because the thread
     * model only involves two threads with little chance for contention.
     */
    private final class EndpointHolder {

        private final Set<EndpointDescription> m_currentServices = new HashSet<EndpointDescription>();
        private final Set<EndpointDescription> m_updatedServices = new HashSet<EndpointDescription>();
        private final URL m_endpoint;

        private boolean m_closed = false;
        private long m_modifiedSince = -1l;

        public EndpointHolder(URL url) {
            m_endpoint = url;
        }
        
        public URL getEndpointUrl() {
            return m_endpoint;
        }
        
        public synchronized void update() {
            m_discovery.logDebug("Updating discovery endpoint: %s", m_endpoint);
            if (m_closed) {
                m_discovery.logDebug("* closed");
                return;
            }

            if (!getUpdatedServicesIfModifiedSince()) {
                m_discovery.logDebug("* not modified");
                return;
            }

            // Call removed for previously discovered endpoints that are no longer
            // part of the updates list.
            for (EndpointDescription currentService : m_currentServices) {
                if (!m_updatedServices.contains(currentService)) {
                    m_discovery.logDebug("* removed: %s", currentService);
                    m_discovery.removeDiscoveredEndpoint(currentService);
                }
            }

            // Call added for ALL discovered endpoints. This allows the discovery
            // to figure out whether it is a previously discovered endpoint and
            // whether is was updated or not.
            m_currentServices.clear();
            m_currentServices.addAll(m_updatedServices);
            m_updatedServices.clear();
            for (EndpointDescription currentService : m_currentServices) {
                m_discovery.logDebug("* added: %s", currentService);
                m_discovery.addDiscoveredEndpoint(currentService);
            }
        }

        public synchronized void close() {
            m_discovery.logDebug("Closing discovery endpoint: %s", m_endpoint);
            m_closed = true;
            for (EndpointDescription service : m_currentServices) {
                m_discovery.removeDiscoveredEndpoint(service);
            }
            m_currentServices.clear();
            m_updatedServices.clear();
        }

        private boolean getUpdatedServicesIfModifiedSince() {
            boolean modified = false;
            HttpURLConnection connection = null;
            Reader reader = null;
            try {
                connection = (HttpURLConnection) m_endpoint.openConnection();
                connection.setRequestMethod("GET");
                if (m_modifiedSince > 0) {
                    connection.setIfModifiedSince(m_modifiedSince);
                }
                connection.setConnectTimeout(m_configuration.getConnectTimeout());
                connection.setReadTimeout(m_configuration.getReadTimeout());
                connection.connect();

                switch (connection.getResponseCode()) {
                    case HttpServletResponse.SC_NOT_MODIFIED:
                        // If the other side signals nothing changed we simply
                        // return this fact to the caller.
                        modified = false;
                        break;
                    case HttpServletResponse.SC_OK:
                        // If the other side signals on OK we update local state
                        // and signal modified to the caller.
                        reader = new InputStreamReader(connection.getInputStream());
                        m_updatedServices.addAll(readEndpoints(reader));
                        m_modifiedSince = connection.getLastModified();
                        modified = true;
                        break;
                    default:
                        // Upon any other response code signal a modified to wipe
                        // all discovered services
                        m_modifiedSince = -1l;
                        modified = true;
                }
            }
            catch (Exception e) {
                // Upon any exception during IO signal a modified to wipe
                // all discovered services
                m_modifiedSince = -1l;
                modified = true;
            }
            finally {
                IOUtil.closeSilently(reader);
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return modified;
        }
    }
}
