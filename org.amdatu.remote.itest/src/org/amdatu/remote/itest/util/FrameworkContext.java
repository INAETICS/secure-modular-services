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
package org.amdatu.remote.itest.util;

import static org.amdatu.remote.itest.util.ITestUtil.join;
import static org.amdatu.remote.itest.util.ITestUtil.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.amdatu.remote.ServiceUtil;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class FrameworkContext {

    private static final int SERVICE_TIMEOUT_DEFAULT = 30;
    private static final int LOG_LEVEL_DEFAULT = LogService.LOG_WARNING;

    private final List<Bundle> m_installedBundles = new ArrayList<Bundle>();

    private final String m_name;
    private final BundleContext m_bundleContext;
    private final DependencyManager m_dependencyManager;

    private volatile Framework m_framework;

    private int m_logLevel = LOG_LEVEL_DEFAULT;
    private long m_serviceTimeout = SERVICE_TIMEOUT_DEFAULT;

    private InternalConfigurationManager m_configManager;
    private InternalLogService m_logService;

    /**
     * Create a Framework Context that wraps a Bundle Context. This is used for the parent framework.
     * Calling {@link #destroy()} will not shut down the actual OSGi framework.
     * 
     * @param name The Framework Context name
     * @param bundleContext The context
     */
    public FrameworkContext(String name, BundleContext bundleContext) throws Exception {
        m_name = name;
        m_bundleContext = bundleContext;
        m_dependencyManager = new DependencyManager(m_bundleContext);

        m_configManager = new InternalConfigurationManager(m_bundleContext, SERVICE_TIMEOUT_DEFAULT);
        m_logService = new InternalLogService(m_bundleContext, m_name, LOG_LEVEL_DEFAULT);
    }

    /**
     * Create a Framework Context that wraps an OSGi Framework. This is used for child frameworks.
     * Calling {@link #destroy()} will shut down the actual OSGi framework.
     * 
     * @param name The Framework Context name
     * @param framewokr The OSGi Framework
     */
    public FrameworkContext(String name, Framework framework) throws Exception {
        m_name = name;
        m_framework = framework;
        m_framework.start();

        for (int i = 0; i < 100 && (framework.getState() != Framework.ACTIVE); i++) {
            Thread.sleep(10);
            if (i >= 99) {
                throw new IllegalStateException("Failed to start framework");
            }
        }

        m_bundleContext = m_framework.getBundleContext();
        m_dependencyManager = new DependencyManager(m_bundleContext);

        m_configManager = new InternalConfigurationManager(m_bundleContext, SERVICE_TIMEOUT_DEFAULT);
        m_logService = new InternalLogService(m_bundleContext, m_name, LOG_LEVEL_DEFAULT);
    }

    public String getName() {
        return m_name;
    }

    public BundleContext getBundleContext() {
        return m_bundleContext;
    }

    public String getFrameworkUUID() {
        return ServiceUtil.getFrameworkUUID(getBundleContext());
    }

    public DependencyManager getDependencyManager() {
        return m_dependencyManager;
    }

    public LogService getLogService() {
        return m_logService;
    }

    public int getLogLevel() {
        return m_logService.getLogLevel();
    }

    public void setLogLevel(int level) {
        m_logService.setLogLevel(level);
    }

    public final long getServiceTimeout() {
        return m_serviceTimeout;
    }

    public final void setServiceTimout(long millis) {
        m_serviceTimeout = millis;
        m_configManager.setTimout(millis);
    }

    public Bundle[] installBundles(String... bundlePaths) throws Exception {
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (String bundlePath : join(",", bundlePaths).split(",")) {
            getLogService().log(LogService.LOG_DEBUG, "Installing bundle location: " + bundlePath);
            try (InputStream fis = new FileInputStream(new File(bundlePath))) {
                Bundle bundle = getBundleContext().installBundle(bundlePath, fis);
                bundles.add(bundle);
                getLogService().log(LogService.LOG_DEBUG,
                    "Installed bundle: " + bundle.getSymbolicName() + "/" + bundle.getVersion());
            }
        }
        m_installedBundles.addAll(bundles);
        return bundles.toArray(new Bundle[bundles.size()]);
    }

    public Bundle[] startBundles(String... bundlePaths) throws Exception {
        Bundle[] bundles = installBundles(bundlePaths);
        for (Bundle bundle : bundles) {
            getLogService().log(LogService.LOG_DEBUG,
                "Starting bundle: " + bundle.getSymbolicName() + "/" + bundle.getVersion());
            bundle.start();
            getLogService().log(LogService.LOG_DEBUG,
                "Started bundle: " + bundle.getSymbolicName() + "/" + bundle.getVersion());
        }
        return bundles;
    }

    public void destroy() throws Exception {
        for (Bundle bundle : m_installedBundles) {
            if (bundle.getState() != Bundle.ACTIVE) {
                continue;
            }
            getLogService().log(LogService.LOG_DEBUG,
                "Stopping bundle: " + bundle.getSymbolicName() + "/" + bundle.getVersion());
            try {
                bundle.stop();
                getLogService().log(LogService.LOG_DEBUG,
                    "Stopped bundle: " + bundle.getSymbolicName() + "/" + bundle.getVersion());
            }
            catch (Exception e) {
                getLogService().log(LogService.LOG_ERROR,
                    "Exception stopping bundle : " + bundle.getSymbolicName() + "/" + bundle.getVersion(), e);
            }
        }
        m_installedBundles.clear();
        m_configManager.destroy();
        m_logService.destroy();

        // child framework only
        if (m_framework != null) {
            m_framework.stop();
            FrameworkEvent result = m_framework.waitForStop(10000);
            if (FrameworkEvent.STOPPED != result.getType()) {
                System.err.println("[WARNING] Framework did not stop within 10 seconds...");
                if (result.getThrowable() != null) {
                    result.getThrowable().printStackTrace();
                }
            }
            m_framework = null;
        }
    }

    public void configure(String pid, String... configuration) throws Exception {
        m_configManager.configure(pid, properties(configuration));
    }

    public String configureFactory(String factoryPid, String... configuration) throws Exception {
        return m_configManager.configureFactory(factoryPid, properties(configuration));
    }

    public <T> T getService(Class<T> serviceClass) {
        try {
            return getService(serviceClass, null);
        }
        catch (InvalidSyntaxException e) {
            return null;
            // Will not happen, since we don't pass in a filter.
        }
    }

    public <T> T getService(Class<T> serviceClass, String filterString) throws InvalidSyntaxException {
        return getService(serviceClass, filterString, m_serviceTimeout);
    }

    public <T> T getService(Class<T> serviceClass, String filterString, long timeout) throws InvalidSyntaxException {
        T serviceInstance = null;

        ServiceTracker<T, ? extends T> serviceTracker;
        if (filterString == null) {
            serviceTracker = new ServiceTracker<T, T>(m_bundleContext, serviceClass.getName(), null);
        }
        else {
            String classFilter = "(" + Constants.OBJECTCLASS + "=" + serviceClass.getName() + ")";
            filterString = "(&" + classFilter + filterString + ")";
            serviceTracker =
                new ServiceTracker<T, T>(m_bundleContext, m_bundleContext.createFilter(filterString), null);
        }
        serviceTracker.open();
        try {
            serviceInstance = serviceTracker.waitForService(timeout);
            if (serviceInstance == null) {
                throw new ServiceException("Service not available: " + serviceClass.getName() + " " + filterString);
            }
            else {
                return serviceInstance;
            }
        }
        catch (InterruptedException e) {
            throw new ServiceException("Service not available: " + serviceClass.getName() + " " + filterString);
        }
    }

    public <T> ServiceReference<T> getServiceReference(Class<T> serviceClass) {
        try {
            return getServiceReference(serviceClass, null);
        }
        catch (InvalidSyntaxException e) {
            return null;
            // Will not happen, since we don't pass in a filter.
        }
    }

    public <T> ServiceReference<T> getServiceReference(Class<T> serviceClass, String filterString)
        throws InvalidSyntaxException {
        return getServiceReference(serviceClass, filterString, m_serviceTimeout);
    }

    public <T> ServiceReference<T> getServiceReference(Class<T> serviceClass, String filterString, long timeout)
        throws InvalidSyntaxException {
        ServiceTracker<T, ? extends T> serviceTracker;
        if (filterString == null) {
            serviceTracker = new ServiceTracker<T, T>(m_bundleContext, serviceClass.getName(), null);
        }
        else {
            String classFilter = "(" + Constants.OBJECTCLASS + "=" + serviceClass.getName() + ")";
            filterString = "(&" + classFilter + filterString + ")";
            serviceTracker =
                new ServiceTracker<T, T>(m_bundleContext, m_bundleContext.createFilter(filterString), null);
        }
        serviceTracker.open();
        try {
            serviceTracker.waitForService(timeout);

            ServiceReference<T> serviceReference = serviceTracker.getServiceReference();

            serviceTracker.close();

            if (serviceReference == null) {
                throw new ServiceException("Service not available: " + serviceClass.getName() + " " + filterString);
            }
            else {
                return serviceReference;
            }
        }
        catch (InterruptedException e) {
            throw new ServiceException("Service not available: " + serviceClass.getName() + " " + filterString);
        }
    }

    /*
     * Internal ConfigurationManager that provides blocking and synchronous configuration updates of Managed Services,
     */
    private static class InternalConfigurationManager {

        private final ConcurrentHashMap<String, ManagedService> m_managedServices = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Dictionary<String, Object>> m_managedServiceConfigs =
            new ConcurrentHashMap<>();

        private final ConcurrentHashMap<String, ManagedServiceFactory> m_managedServiceFactories =
            new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Dictionary<String, Object>> m_managedServiceFactoryConfigs =
            new ConcurrentHashMap<>();

        private final BundleContext m_context;
        private long m_timeout;

        private ServiceTracker<ManagedService, ManagedService> m_managedServiceTracker;
        private ServiceTracker<ManagedServiceFactory, ManagedServiceFactory> m_managedServiceFactoryTracker;

        public InternalConfigurationManager(BundleContext context, long timeout) throws Exception {
            m_context = context;
            m_timeout = timeout;

            Filter managedServiceFilter =
                FrameworkUtil.createFilter(String.format("(&(objectClass=%s)(service.pid=*))",
                    ManagedService.class.getName()));

            m_managedServiceTracker =
                new ServiceTracker<ManagedService, ManagedService>(m_context, managedServiceFilter,
                    new ServiceTrackerCustomizer<ManagedService, ManagedService>() {

                        @Override
                        public ManagedService addingService(ServiceReference<ManagedService> reference) {

                            String pid = (String) reference.getProperty("service.pid");
                            ManagedService service = m_context.getService(reference);
                            if (pid == null || service == null) {
                                return null;
                            }
                            m_managedServices.put(pid, service);

                            Dictionary<String, Object> config = m_managedServiceConfigs.get(pid);
                            try {
                                service.updated(config);
                            }
                            catch (ConfigurationException e) {
                                e.printStackTrace();
                            }
                            return service;
                        }

                        @Override
                        public void modifiedService(ServiceReference<ManagedService> reference, ManagedService service) {
                            // Nothing to do, unless someone changed the pid....
                        }

                        @Override
                        public void removedService(ServiceReference<ManagedService> reference, ManagedService service) {
                            String pid = (String) reference.getProperty("service.pid");
                            if (pid != null) {
                                m_managedServices.remove(pid);
                            }
                        }
                    });

            m_managedServiceTracker.open();

            Filter managedServiceFactoryFilter =
                m_context.createFilter(String.format("(&(objectClass=%s)(service.pid=*))",
                    ManagedServiceFactory.class.getName()));

            m_managedServiceFactoryTracker =
                new ServiceTracker<ManagedServiceFactory, ManagedServiceFactory>(m_context,
                    managedServiceFactoryFilter,
                    new ServiceTrackerCustomizer<ManagedServiceFactory, ManagedServiceFactory>() {

                        @Override
                        public ManagedServiceFactory addingService(ServiceReference<ManagedServiceFactory> reference) {

                            String pid = (String) reference.getProperty("service.pid");
                            ManagedServiceFactory service = m_context.getService(reference);
                            if (pid == null || service == null) {
                                return null;
                            }
                            m_managedServiceFactories.put(pid, service);

                            Dictionary<String, Object> cfg = m_managedServiceConfigs.get(pid);
                            try {
                                service.updated(pid, cfg);
                            }
                            catch (ConfigurationException e) {
                                e.printStackTrace();
                            }
                            return service;
                        }

                        @Override
                        public void modifiedService(ServiceReference<ManagedServiceFactory> reference,
                            ManagedServiceFactory service) {
                            // Nothing to do, unless someone changed the pid....
                        }

                        @Override
                        public void removedService(ServiceReference<ManagedServiceFactory> reference,
                            ManagedServiceFactory service) {
                            String pid = (String) reference.getProperty("service.pid");
                            m_managedServiceFactories.remove(pid);
                        }
                    });

            m_managedServiceFactoryTracker.open();
        }

        public void destroy() throws Exception {
            m_managedServiceTracker.close();
            m_managedServiceFactoryTracker.close();
            m_managedServiceConfigs.clear();
            m_managedServices.clear();
            m_managedServiceFactoryConfigs.clear();
            m_managedServiceFactories.clear();
        }

        public final long getTimeout() {
            return m_timeout;
        }

        public final void setTimout(long millis) {
            m_timeout = millis;
        }

        public void configure(String pid, Dictionary<String, Object> configuration) throws Exception {
            m_managedServiceConfigs.put(pid, configuration);
            long wait = 0l;
            ManagedService service = m_managedServices.get(pid);
            while (service == null && wait <= m_timeout) {
                Thread.sleep(10);
                wait += 10;
            }
            if (service == null) {
                throw new IllegalStateException("Timed out while waiting for managed service " + pid);
            }
            service.updated(configuration);
        }

        public String configureFactory(String pid, Dictionary<String, Object> configuration) throws Exception {
            m_managedServiceFactoryConfigs.put(pid, configuration);
            String configPid = UUID.randomUUID().toString();
            long wait = 0l;
            ManagedServiceFactory service = m_managedServiceFactories.get(pid);
            while (service == null && wait <= m_timeout) {
                Thread.sleep(10);
                wait += 10;
            }
            if (service == null) {
                throw new IllegalStateException("Timed out while waiting for managed service factory " + pid);
            }
            service.updated(configPid, configuration);
            return configPid;
        }
    }

    /**
     * Internal LogService that logs to the console
     */
    private static class InternalLogService implements LogService {

        private final BundleContext m_context;
        private final String m_name;
        private int m_level;

        private ServiceRegistration<?> m_registration;

        public InternalLogService(BundleContext context, String name, int level) {
            m_context = context;
            m_name = name;
            m_level = level;

            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(Constants.SERVICE_RANKING, Integer.valueOf(1000));
            m_registration =
                m_context.registerService(LogService.class.getName(), this, properties);
        }

        public void destroy() {
            m_registration.unregister();
        }

        public int getLogLevel() {
            return m_level;
        }

        public void setLogLevel(int level) {
            m_level = level;
        }

        @Override
        public void log(int level, String message) {
            log(null, level, message, null);
        }

        @Override
        public void log(int level, String message, Throwable exception) {
            log(null, level, message, exception);
        }

        @Override
        public void log(@SuppressWarnings("rawtypes") ServiceReference serviceReference, int level, String message) {
            log(serviceReference, level, message, null);
        }

        @Override
        public void log(@SuppressWarnings("rawtypes") ServiceReference serviceReference, int level, String message,
            Throwable exception) {
            if (level <= m_level) {
                System.out.println("[" + m_name + "] " +
                    (serviceReference == null ? "" : serviceReference + " ") +
                    getLevel(level) + " " +
                    message + " " +
                    (exception == null ? "" : exception));
            }
        }

        private String getLevel(int level) {
            switch (level) {
                case 1:
                    return "[ERROR]";
                case 2:
                    return "[WARN ]";
                case 3:
                    return "[INFO ]";
                case 4:
                    return "[DEBUG]";
                default:
                    return "[?????]";
            }
        }
    }
}