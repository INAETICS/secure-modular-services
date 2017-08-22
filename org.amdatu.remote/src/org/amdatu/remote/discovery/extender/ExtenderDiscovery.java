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
package org.amdatu.remote.discovery.extender;

import static org.amdatu.remote.EndpointUtil.readEndpoints;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.amdatu.remote.Constants;
import org.amdatu.remote.discovery.AbstractDiscovery;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Extender based discovery for OSGi RemoteServiceAdmin. *
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class ExtenderDiscovery extends AbstractDiscovery implements BundleListener {

    public static final String DISCOVERY_NAME = "Amdatu Remote - Discovery (Extender)";
    public static final String DISCOVERY_TYPE = "extender";

    private final ConcurrentHashMap<Bundle, List<EndpointDescription>> m_bundleDescriptions =
        new ConcurrentHashMap<Bundle, List<EndpointDescription>>();

    public ExtenderDiscovery() {
        super("extender");
    }

    @Override
    public void startComponent() throws Exception {
        super.startComponent();
        getBundleContext().addBundleListener(this);
        for (Bundle bundle : getBundleContext().getBundles()) {
            if (bundle.getState() == Bundle.ACTIVE) {
                registerRemoteServices(bundle);
            }
        }
    }

    @Override
    public void stopComponent() throws Exception {
        getBundleContext().removeBundleListener(this);
        for (Bundle bundle : m_bundleDescriptions.keySet()) {
            unregisterRemoteServices(bundle);
        }
        super.stopComponent();
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
                registerRemoteServices(event.getBundle());
                break;
            case BundleEvent.LAZY_ACTIVATION:
                registerRemoteServices(event.getBundle());
                break;
            case BundleEvent.STOPPING:
                unregisterRemoteServices(event.getBundle());
                break;
            case BundleEvent.STOPPED:
                // Fall-back as STOPPPING only applies to bundles with an activator
                unregisterRemoteServices(event.getBundle());
                break;
            default:
                break;
        }
    }

    @Override
    protected void addPublishedEndpoint(EndpointDescription endpointDescription, String matchedFilter) {
        throw new IllegalStateException("Extender based discovery can not publish endpoints");
    }

    @Override
    protected void removePublishedEndpoint(EndpointDescription endpointDescription, String matchedFilter) {
        throw new IllegalStateException("Extender based discovery can not publish endpoints");
    }

    @Override
    protected void modifyPublishedEndpoint(EndpointDescription endpoint, String matchedFilter) {
        throw new IllegalStateException("Extender based discovery can not publish endpoints");
    }

    private void registerRemoteServices(Bundle bundle) {
        logDebug("Registering Remotes Service endpoints start: %s", bundle);
        try {
            String remoteServiceHeader = getBundleHeader(bundle, Constants.MANIFEST_REMOTE_SERVICE_HEADER);
            if (remoteServiceHeader == null) {
                logDebug("No Remote-Service header found: %s", bundle);
                return;
            }

            String[] resourcePaths = getRemoteServicePaths(remoteServiceHeader);
            if (resourcePaths == null) {
                logDebug("No Remote-Service resources found: %s", bundle);
                return;
            }

            List<EndpointDescription> descriptions = new ArrayList<EndpointDescription>();
            for (String resourcePath : resourcePaths) {
                InputStream input = bundle.getEntry(resourcePath).openStream();
                try {
                    List<EndpointDescription> result = readEndpoints(new InputStreamReader(input));
                    descriptions.addAll(result);
                }
                finally {
                    input.close();
                }
            }
            m_bundleDescriptions.put(bundle, descriptions);

            for (EndpointDescription description : descriptions) {
                logDebug("Registering Remotes Service endpoints done: %s", description);
                addDiscoveredEndpoint(description);
            }

            logDebug("Registering Remotes Service endpoints done: %s", bundle);
        }
        catch (Exception e) {
            logWarning("Registering Remote Service endpoints failed: %s", e, bundle);
        }
    }

    private void unregisterRemoteServices(Bundle bundle) {
        logDebug("Unregistering Remotes Service endpoints start: %s", bundle);
        try {
            List<EndpointDescription> descriptions = m_bundleDescriptions.remove(bundle);
            if (descriptions != null) {
                for (EndpointDescription description : descriptions) {
                    removeDiscoveredEndpoint(description);
                }
            }
            logDebug("Unregistering Remotes Service endpoints done: %s", bundle);
        }
        catch (Exception e) {
            logDebug("Unregistering Remotes Service endpoints failed: %s", e, bundle);
        }
    }

    private static String[] getRemoteServicePaths(String headerValue) {
        List<String> paths = new ArrayList<String>();
        String[] clauses = headerValue.split(",");
        for (String clause : clauses) {
            clause = clause.trim();
            if (clause.indexOf(";") != -1) {
                // spec has no architected parameters
                clause = clause.substring(0, clause.indexOf(";"));
            }
            paths.add(clause);
        }
        return paths.toArray(new String[paths.size()]);
    }

    private static String getBundleHeader(Bundle bundle, String key) {
        Enumeration<String> headerKeys = bundle.getHeaders().keys();
        while (headerKeys.hasMoreElements()) {
            String headerKey = headerKeys.nextElement();
            if (headerKey.equals(key)) {
                return bundle.getHeaders().get(headerKey);
            }
        }
        return null;
    }
}
