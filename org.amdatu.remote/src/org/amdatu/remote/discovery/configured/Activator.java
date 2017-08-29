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
package org.amdatu.remote.discovery.configured;

import static org.amdatu.remote.ServiceUtil.getConfigIntValue;
import static org.amdatu.remote.ServiceUtil.getConfigStringValue;
import static org.amdatu.remote.discovery.DiscoveryUtil.createEndpointListenerServiceProperties;

import java.net.URL;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import org.amdatu.remote.discovery.AbstractNoEncryptionActivator;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("deprecation")
public class Activator extends AbstractNoEncryptionActivator implements ConfiguredDiscoveryConfiguration, ManagedService {

    public static final String CONFIG_PID = "org.amdatu.remote.discovery.configured";
    public static final String CONFIG_HOST_KEY = CONFIG_PID + ".host";
    public static final String CONFIG_PORT_KEY = CONFIG_PID + ".port";
    public static final String CONFIG_PATH_KEY = CONFIG_PID + ".path";
    public static final String CONFIG_SCHEDULE_KEY = CONFIG_PID + ".schedule";
    public static final String CONFIG_ENDPOINTS_KEY = CONFIG_PID + ".endpoints";
    public static final String CONFIG_CONNECT_TIMEOUT_KEY = CONFIG_PID + ".connecttimeout";
    public static final String CONFIG_READ_TIMEOUT_KEY = CONFIG_PID + ".readtimeout";

    private volatile BundleContext m_bundleContext;
    private volatile DependencyManager m_dependencyManager;

    private volatile Component m_configuration;
    private volatile Component m_discovery;

    private volatile URL m_baseUrl;
    private volatile int m_schedule;
    private volatile Set<URL> m_endpoints;
    private volatile int m_connectTimeout;
    private volatile int m_readTimeout;

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {

        m_bundleContext = context;
        m_dependencyManager = manager;

        URL baseUrl = getConfiguredBaseUrl(null);
        int schedule = getConfiguredPollSchedule(null);
        Set<URL> endpoints = getConfiguredEndpoints(null);
        int connectTimeout = getConfigIntValue(context, CONFIG_CONNECT_TIMEOUT_KEY, null, DEFAULT_CONNECT_TIMEOUT);
        int readTimeout = getConfigIntValue(context, CONFIG_READ_TIMEOUT_KEY, null, DEFAULT_READ_TIMEOUT);

        m_baseUrl = baseUrl;
        m_schedule = schedule;
        m_endpoints = endpoints;
        m_connectTimeout = connectTimeout;
        m_readTimeout = readTimeout;
        
        initEncryption(context);

        registerDiscoveryService();
        registerConfigurationService();
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {

        unregisterConfigurationService();
        unregisterDiscoveryService();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

        BundleContext context = getBundleContext();

        try {
            URL baseUrl = getConfiguredBaseUrl(properties);
            int schedule = getConfiguredPollSchedule(properties);
            Set<URL> endpoints = getConfiguredEndpoints(properties);
            int connectTimeout =
                getConfigIntValue(context, CONFIG_CONNECT_TIMEOUT_KEY, properties, DEFAULT_CONNECT_TIMEOUT);
            int readTimeout = getConfigIntValue(context, CONFIG_READ_TIMEOUT_KEY, properties, DEFAULT_READ_TIMEOUT);

            m_connectTimeout = connectTimeout;
            m_readTimeout = readTimeout;

            if (!baseUrl.equals(m_baseUrl) || m_schedule != schedule || !m_endpoints.containsAll(endpoints)
                || !endpoints.containsAll(m_endpoints)) {
                unregisterDiscoveryService();
                m_baseUrl = baseUrl;
                m_schedule = schedule;
                m_endpoints = endpoints;
                registerDiscoveryService();
            }

        }
        catch (Exception e) {
            throw new ConfigurationException("unknown", e.getMessage(), e);
        }
    }

    private void registerDiscoveryService() {

        Properties properties =
            createEndpointListenerServiceProperties(m_dependencyManager.getBundleContext(),
                ConfiguredEndpointDiscovery.DISCOVERY_TYPE);

        ConfiguredEndpointDiscovery discovery = new ConfiguredEndpointDiscovery(this);

        Component component = createComponent()
            .setInterface(new String[] { EndpointEventListener.class.getName(), EndpointListener.class.getName() },
                properties)
            .setImplementation(discovery)
            .add(createServiceDependency()
                .setService(HttpService.class)
                .setRequired(true))
            .add(createServiceDependency()
                .setService(EndpointEventListener.class)
                .setCallbacks("eventListenerAdded", "eventListenerModified", "eventListenerRemoved")
                .setRequired(false))
            .add(createServiceDependency()
                .setService(EndpointListener.class)
                .setCallbacks("listenerAdded", "listenerModified", "listenerRemoved")
                .setRequired(false))
            .add(createServiceDependency()
                .setService(LogService.class)
                .setRequired(false));

        m_discovery = component;
        m_dependencyManager.add(m_discovery);
    }

    private void unregisterDiscoveryService() {

        Component component = m_discovery;
        m_discovery = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }
    }

    private void registerConfigurationService() {

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, CONFIG_PID);

        Component component = createComponent()
            .setInterface(ManagedService.class.getName(), properties)
            .setImplementation(this)
            .setAutoConfig(BundleContext.class, false)
            .setAutoConfig(DependencyManager.class, false)
            .setAutoConfig(Component.class, false);

        m_configuration = component;
        m_dependencyManager.add(component);
    }

    private void unregisterConfigurationService() {

        Component component = m_configuration;
        m_configuration = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }
    }

    private Set<URL> getConfiguredEndpoints(Dictionary<String, ?> properties) throws ConfigurationException {
        String configured = getConfigStringValue(m_bundleContext, CONFIG_ENDPOINTS_KEY, properties, "");
        try {
            Set<URL> endpoints = new HashSet<URL>();
            for (String value : configured.split(",")) {
                String url = value.trim();
                if (!"".equals(url)) {
                    endpoints.add(new URL(url));
                }
            }
            return endpoints;
        }
        catch (Exception e) {
            throw new ConfigurationException(CONFIG_ENDPOINTS_KEY, e.getMessage(), e);
        }
    }

    private int getConfiguredPollSchedule(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigIntValue(m_bundleContext, CONFIG_SCHEDULE_KEY, properties, 10);
    }

    private URL getConfiguredBaseUrl(Dictionary<String, ?> properties) throws ConfigurationException {

        String host = getConfigStringValue(m_bundleContext, CONFIG_HOST_KEY, properties, null);
        if (host == null) {
            host = getConfigStringValue(m_bundleContext, "org.apache.felix.http.host", properties, "localhost");
        }

        int port = getConfigIntValue(m_bundleContext, CONFIG_PORT_KEY, properties, -1);
        if (port == -1) {
            port = getConfigIntValue(m_bundleContext, "org.osgi.service.http.port", properties, 8080);
        }

        String path = getConfigStringValue(m_bundleContext, CONFIG_PATH_KEY, properties, CONFIG_PID);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        try {
            return new URL("http", host, port, path);
        }
        catch (Exception e) {
            throw new ConfigurationException("unknown", e.getMessage(), e);
        }
    }

    @Override
    public URL getBaseUrl() {
        return m_baseUrl;
    }

    @Override
    public int getConnectTimeout() {
        return m_connectTimeout;
    }

    @Override
    public int getReadTimeout() {
        return m_readTimeout;
    }

    @Override
    public int getSchedule() {
        return m_schedule;
    }

    @Override
    public Set<URL> getEndpoints() {
        return m_endpoints;
    }

}
