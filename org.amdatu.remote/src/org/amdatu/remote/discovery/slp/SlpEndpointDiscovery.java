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
package org.amdatu.remote.discovery.slp;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.amdatu.remote.discovery.AbstractHttpEndpointDiscovery;
import org.amdatu.remote.discovery.HttpEndpointDiscoveryConfiguration;
import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLP;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.sa.ServiceAgent;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.ua.UserAgentClient;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * SLP implementation of service endpoint based discovery. This type of discovery discovers HTTP endpoints
 * that provide published services based on the {@link EndpointDescription} extender format.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class SlpEndpointDiscovery extends AbstractHttpEndpointDiscovery<HttpEndpointDiscoveryConfiguration> {

    public static final String DISCOVERY_NAME = "Amdatu Remote Service Endpoint (SLP)";
    public static final String DISCOVERY_TYPE = "slp";

    private final static String SERVICE_NAME = "service:osgi.remote:http";
    private final static ServiceType SERVICE_TYPE = new ServiceType(SERVICE_NAME);
    private final static String SERVICE_LANGUAGES = Locale.ENGLISH.getLanguage();

    private final Map<URL, ServiceInfo> m_services = new HashMap<URL, ServiceInfo>();

    private volatile ServiceAgent m_agent;
    private volatile UserAgentClient m_client;
    private volatile SlpUpdater m_updater;

    public SlpEndpointDiscovery(HttpEndpointDiscoveryConfiguration configuration) {
        super(DISCOVERY_TYPE, configuration);
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Keys.PORT_KEY.getKey(), 9999);
        properties.put(Keys.UA_UNICAST_PREFER_TCP.getKey(), "true");

        if (m_agent != null && m_agent.isRunning()) {
            m_agent.stop();
        }
        m_agent = createServiceAgent(this.getClass().getClassLoader(), properties);
        m_agent.start();
        m_client = createUserAgentClient(this.getClass().getClassLoader(), properties);

        String url = "service:osgi.remote:" + getConfiguration().getBaseUrl().toExternalForm();
        ServiceURL serviceURL = new ServiceURL(url, 10);
        ServiceInfo serviceInfo = new ServiceInfo(serviceURL, SERVICE_LANGUAGES, Scopes.DEFAULT, Attributes.NONE);
        m_agent.register(serviceInfo);

        m_updater = new SlpUpdater();
    }

    @Override
    protected void stopComponent() throws Exception {
        try {
            m_agent.stop();
        }
        catch (Exception e) {
            logWarning("Exception while stopping", e);
        }
        m_agent = null;
        m_client = null;

        try {
            m_updater.cancel();
        }
        catch (Exception e) {
            logWarning("Exception while stopping", e);
        }
        m_updater = null;

        for (ServiceInfo service : m_services.values()) {
            removeDiscoveryEndpoint(service.getKey().toString());
        }
        m_services.clear();

        super.stopComponent();
    }

    /**
     * Create a {@link ServiceAgent} using the specified arguments.
     *
     * @param cl the classLoader
     * @param port the port
     * @return the agent
     */
    private static ServiceAgent createServiceAgent(ClassLoader cl, Dictionary<String, Object> properties) {
        MapSettings settings = getSettings(properties);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return SLP.newServiceAgent(settings);
        }
        catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
        finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    /**
     * Create a {@link UserAgentClient} using the specified arguments.
     *
     * @param cl
     * @param properties
     * @return the client
     */
    private static UserAgentClient createUserAgentClient(ClassLoader cl, Dictionary<String, Object> properties) {

        MapSettings settings = getSettings(properties);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return SLP.newUserAgentClient(settings);
        }
        finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    private static MapSettings getSettings(Dictionary<String, ?> properties) {
        MapSettings settings = new MapSettings();
        Object slpPort = properties.get(Keys.PORT_KEY.getKey());
        if (slpPort != null) {
            settings.put(Keys.PORT_KEY, Keys.PORT_KEY.convert(slpPort));
        }
        Object slpScopes = properties.get(Keys.SCOPES_KEY.getKey());
        if (slpScopes != null) {
            settings.put(Keys.SCOPES_KEY, Keys.SCOPES_KEY.convert(slpScopes));
        }
        Object slpAddress = properties.get(Keys.ADDRESSES_KEY.getKey());
        if (slpAddress != null) {
            settings.put(Keys.ADDRESSES_KEY, Keys.ADDRESSES_KEY.convert(slpAddress));
        }
        Object slpMtu = properties.get(Keys.MAX_TRANSMISSION_UNIT_KEY.getKey());
        if (slpMtu != null) {
            settings.put(Keys.MAX_TRANSMISSION_UNIT_KEY, Keys.MAX_TRANSMISSION_UNIT_KEY.convert(slpMtu));
        }
        Object tcp = properties.get(Keys.UA_UNICAST_PREFER_TCP.getKey());
        if (tcp != null) {
            settings.put(Keys.UA_UNICAST_PREFER_TCP, Keys.UA_UNICAST_PREFER_TCP.convert(tcp));
        }
        return settings;
    }

    private class SlpUpdater implements Runnable {

        private final ScheduledExecutorService m_executor;
        private final ScheduledFuture<?> m_future;

        public SlpUpdater() {
            m_executor = Executors.newSingleThreadScheduledExecutor();
            m_future = m_executor.scheduleAtFixedRate(this, 1, 5, TimeUnit.SECONDS);
        }

        public void cancel() {
            m_future.cancel(false);
            m_executor.shutdownNow();
        }

        @Override
        public void run() {
            UserAgentClient userAgentClient = m_client;
            if (userAgentClient == null) {
                return;
            }

            List<ServiceInfo> services =
                userAgentClient.findServices(SERVICE_TYPE, SERVICE_LANGUAGES, Scopes.NONE, null);
            for (ServiceInfo service : services) {
                service.setRegistered(true);
                try {
                    URL url = new URL(service.getServiceURL().getURL().replaceFirst(SERVICE_NAME, "http"));
                    if (!m_services.containsKey(url)) {
                        addDiscoveryEndpoint(service.getKey().toString(), url);
                    }
                    m_services.put(url, service);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            long now = System.currentTimeMillis();
            List<String> removeIds = new ArrayList<String>();
            for (ServiceInfo serviceInfo : m_services.values()) {
                if (serviceInfo.isExpiredAsOf(now)) {
                    removeIds.add(serviceInfo.getKey().toString());
                }
            }
            for (String removeId : removeIds) {
                m_services.remove(removeId);
                removeDiscoveryEndpoint(removeId);
            }
        }
    }
}
