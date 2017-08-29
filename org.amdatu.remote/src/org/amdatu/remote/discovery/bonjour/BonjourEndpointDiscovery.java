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
package org.amdatu.remote.discovery.bonjour;

import java.net.InetAddress;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.amdatu.remote.discovery.AbstractHttpEndpointDiscovery;
import org.amdatu.remote.discovery.HttpEndpointDiscoveryConfiguration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Bonjour implementation of service endpoint based discovery. This type of discovery discovers HTTP endpoints
 * that provide published services based on the {@link EndpointDescription} extender format.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class BonjourEndpointDiscovery extends AbstractHttpEndpointDiscovery<HttpEndpointDiscoveryConfiguration> {

    public static final String DISCOVERY_NAME = "Amdatu Remote Service Endpoint (Bonjour)";
    public static final String DISCOVERY_TYPE = "bonjour";
    public static final String SERVICE_TYPE = "_endpointdiscovery._amdaturs.local.";

    private volatile JmDNS m_jmDNS;
    private volatile ServiceListener m_jmDNSListener;

    public BonjourEndpointDiscovery(HttpEndpointDiscoveryConfiguration configuration) {
        super(DISCOVERY_TYPE, configuration);
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();

        URL localEndpoint = getConfiguration().getBaseUrl();

        InetAddress ip = InetAddress.getByName(localEndpoint.getHost());
        m_jmDNS = JmDNS.create(ip, DISCOVERY_NAME);
        m_jmDNSListener = new JmDNSListener();
        m_jmDNS.addServiceListener(SERVICE_TYPE, m_jmDNSListener);

        Map<String, Object> props = new HashMap<String, Object>();
        String encryptedEndpoint = getConfiguration().encrypt(localEndpoint.getPath());
        props.put("path", encryptedEndpoint);
        ServiceInfo info = ServiceInfo.create(SERVICE_TYPE, DISCOVERY_NAME, localEndpoint.getPort(), 0, 0, props);
        m_jmDNS.registerService(info);
    }

    @Override
    protected void stopComponent() throws Exception {
        try {
            m_jmDNS.removeServiceListener(SERVICE_TYPE, m_jmDNSListener);
        }
        catch (Exception e) {
            logWarning("Exception while stopping", e);
        }

        try {
            m_jmDNS.unregisterAllServices();
        }
        catch (Exception e) {
            logWarning("Exception while stopping", e);
        }

        try {
            m_jmDNS.close();
        }
        catch (Exception e) {
            logWarning("Exception while stopping", e);
        }
        m_jmDNS = null;

        super.stopComponent();
    }

    private class JmDNSListener implements ServiceListener {

        @Override
        public void serviceAdded(ServiceEvent event) {
        }

        @Override
        public void serviceRemoved(ServiceEvent serviceEvent) {
            ServiceInfo serviceInfo = serviceEvent.getInfo();
            for (String enc_url : serviceInfo.getURLs()) {
                try {
                    String url = getConfiguration().decrypt(enc_url);
                    removeDiscoveryEndpoint(serviceInfo.getKey() + url);
                }
                catch (Exception e) {
                    logError("Failed to parse service URL: %s", e, enc_url);
                }
            }
        }

        @Override
        public void serviceResolved(ServiceEvent serviceEvent) {
            ServiceInfo serviceInfo = serviceEvent.getInfo();
            for (String enc_url : serviceInfo.getURLs()) {
                try {
                    String url = getConfiguration().decrypt(enc_url);
                    URL serviceUrl = new URL(url);
                    addDiscoveryEndpoint(serviceInfo.getKey() + url, serviceUrl);
                }
                catch (Exception e) {
                    logError("Failed to parse service URL: %s", e, enc_url);
                }
            }
        }
    }
}
