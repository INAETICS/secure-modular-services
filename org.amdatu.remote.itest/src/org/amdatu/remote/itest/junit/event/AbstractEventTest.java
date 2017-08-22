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
package org.amdatu.remote.itest.junit.event;

import static org.amdatu.remote.itest.config.Configs.configs;
import static org.amdatu.remote.itest.config.Configs.frameworkConfig;

import org.amdatu.remote.AbstractEndpointPublishingComponent;
import org.amdatu.remote.itest.config.Config;
import org.amdatu.remote.itest.config.FrameworkConfig;
import org.amdatu.remote.itest.junit.RemoteServicesTestBase;
import org.amdatu.remote.itest.util.FrameworkContext;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Base class for common integration tests. It simply starts a single child
 * context and only provisionins the defaults.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("restriction")
public abstract class AbstractEventTest extends RemoteServicesTestBase {

    protected final DummyEndpointPublishingComponent m_dummy = new DummyEndpointPublishingComponent();

    protected volatile ServiceReference<RemoteServiceAdmin> m_remoteServiceAdminReference;
    protected volatile RemoteServiceAdmin m_remoteServiceAdmin;

    protected String m_remoteAdminPropertyKey = "itest.bundles.admin.http";

    @Override
    protected Config[] configureFramework(final FrameworkContext parent) throws Exception {
        BundleContext parentBC = getParentContext().getBundleContext();
        String systemPackages = parentBC.getProperty("itest.systempackages");
        String defaultBundles = parentBC.getProperty("itest.bundles.default");
        String remoteAdminHTTP = parentBC.getProperty(m_remoteAdminPropertyKey);
        String topologyManagerBundles = parentBC.getProperty("itest.bundles.topology.promiscuous");

        parent.setLogLevel(LogService.LOG_DEBUG);
        parent.setServiceTimout(30000);

        FrameworkConfig child1 = frameworkConfig("CHILD1").logLevel(LogService.LOG_DEBUG).serviceTimeout(10000)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .frameworkProperty("org.osgi.service.http.port", "8089")
            .frameworkProperty("org.amdatu.remoteservices.dontuseservicehook", "true")
            .bundlePaths(defaultBundles, remoteAdminHTTP, topologyManagerBundles);

        return configs(child1);
    }

    @Override
    protected void configureServices() throws Exception {
        DependencyManager dm = getChildContext().getDependencyManager();

        dm.add(dm.createComponent().setImplementation(m_dummy)
            .add(dm.createServiceDependency().setService(EndpointEventListener.class, "(!(discovery=true))")
                .setCallbacks("eventListenerAdded", "eventListenerModified", "eventListenerRemoved")
                .setRequired(false)));

        m_remoteServiceAdminReference = getChildContext("CHILD1").getBundleContext()
            .getServiceReference(RemoteServiceAdmin.class);
        for (int i = 0; i < 1000 && m_remoteServiceAdminReference == null; i++) {
            Thread.sleep(10);
            m_remoteServiceAdminReference = getChildContext("CHILD1").getBundleContext()
                .getServiceReference(RemoteServiceAdmin.class);
        }
        assertNotNull("Unable to locate RemoteServiceAdmin reference", m_remoteServiceAdminReference);
        m_remoteServiceAdmin = getChildContext("CHILD1").getBundleContext().getService(m_remoteServiceAdminReference);
    }

    @Override
    protected void cleanupTest() throws Exception {
        if (m_remoteServiceAdminReference != null) {
            getChildContext("CHILD1").getBundleContext().ungetService(m_remoteServiceAdminReference);
            m_remoteServiceAdminReference = null;
        }
        m_remoteServiceAdmin = null;
    }

    protected final FrameworkContext getChildContext() {
        return getChildContext("CHILD1");
    }

    protected final BundleContext getChildBundleContext() {
        return getChildContext("CHILD1").getBundleContext();
    }

    protected final void logDebug(final String message) {
        getChildContext("CHILD1").getLogService().log(LogService.LOG_DEBUG, message);
    }

    protected static class DummyEndpointPublishingComponent extends AbstractEndpointPublishingComponent {

        public DummyEndpointPublishingComponent() {
            super("dummy", "dummy");
        }

        public void callEndpointAdded(final EndpointDescription endpoint) {
            endpointAdded(endpoint);
        }

        public void callEndpointRemoved(final EndpointDescription endpoint) {
            endpointRemoved(endpoint);
        }

        public void callEndpointModified(final EndpointDescription endpoint) {
            endpointModified(endpoint);
        }
    }
}
