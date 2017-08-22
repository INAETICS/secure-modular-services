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
package org.amdatu.remote.itest.junit.full;

import static org.amdatu.remote.itest.config.Configs.configs;
import static org.amdatu.remote.itest.config.Configs.frameworkConfig;
import static org.amdatu.remote.itest.util.ITestUtil.getRandomFreePort;
import static org.mockito.Mockito.mock;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import org.amdatu.remote.admin.itest.api.EchoInterface;
import org.amdatu.remote.itest.config.Config;
import org.amdatu.remote.itest.config.FrameworkConfig;
import org.amdatu.remote.itest.util.BlockingRemoteServiceAdminListener;
import org.amdatu.remote.itest.util.FrameworkContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 *
 */
public class ConfiguredEndpointDiscoveryFullIntegrationTest extends AbstractFullIntegrationTest {

    private String httpPort1;
    private String httpPort2;

    @Override
    protected Config[] configureFramework(final FrameworkContext parent) throws Exception {
        BundleContext parentBC = getParentContext().getBundleContext();

        String systemPackages = parentBC.getProperty("itest.systempackages");
        String defaultBundles = parentBC.getProperty("itest.bundles.default");
        String remoteServiceAdminBundles = parentBC.getProperty(m_remoteAdminPropertyKey);
        String topologyManagerBundles = parentBC.getProperty("itest.bundles.topology.promiscuous");
        String discoveryBundles = parentBC.getProperty("itest.bundles.discovery.configured");

        parent.setLogLevel(LogService.LOG_DEBUG);
        parent.setServiceTimout(30000);

        httpPort1 = getRandomFreePort();
        httpPort2 = getRandomFreePort();

        FrameworkConfig child1 = frameworkConfig("CHILD1")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(30000)
            .frameworkProperty("felix.cm.loglevel", "1")
            .frameworkProperty("org.osgi.service.http.port", httpPort1)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .bundlePaths(defaultBundles, remoteServiceAdminBundles, topologyManagerBundles, discoveryBundles);

        FrameworkConfig child2 = frameworkConfig("CHILD2")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(30000)
            .frameworkProperty("felix.cm.loglevel", "1")
            .frameworkProperty("org.osgi.service.http.port", httpPort2)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .bundlePaths(defaultBundles, remoteServiceAdminBundles, topologyManagerBundles, discoveryBundles);

        return configs(child1, child2);
    }

    @Override
    protected void configureServices() throws Exception {
        getChildContext("CHILD2").configure(m_RSABundleName, "org.amdatu.remote.admin.http.readtimeout",
            "1000");

        getChildContext("CHILD1").configure("org.amdatu.remote.discovery.configured",
            "org.amdatu.remote.discovery.configured.path", "qqq123",
            "org.amdatu.remote.discovery.configured.endpoints", "http://localhost:" + httpPort2 + "/qqq321",
            "org.amdatu.remote.discovery.configured.schedule", "1");

        getChildContext("CHILD2").configure("org.amdatu.remote.discovery.configured",
            "org.amdatu.remote.discovery.configured.path", "qqq321",
            "org.amdatu.remote.discovery.configured.endpoints", "http://localhost:" + httpPort1 + "/qqq123",
            "org.amdatu.remote.discovery.configured.schedule", "1");
    }

    @Override
    protected void cleanupTest() throws Exception {
    }

    public void testExportImport_ExportingRSA() throws Exception {
        doExportImportTest(true,
            getBundle(getChildContext("CHILD1").getBundleContext(), m_RSABundleName));
    }

    public void testExportImport_ImportingRSA() throws Exception {
        doExportImportTest(false,
            getBundle(getChildContext("CHILD2").getBundleContext(), m_RSABundleName));
    }

    public void testExportImport_ExportingTOPO() throws Exception {
        doExportImportTest(true,
            getBundle(getChildContext("CHILD1").getBundleContext(), "org.amdatu.remote.topology.promiscuous"));
    }

    public void testExportImport_ImportingTOPO() throws Exception {
        doExportImportTest(false,
            getBundle(getChildContext("CHILD2").getBundleContext(), "org.amdatu.remote.topology.promiscuous"));
    }

    public void testExportImport_ExportingDISCO() throws Exception {
        doExportImportTest(false,
            getBundle(getChildContext("CHILD1").getBundleContext(), "org.amdatu.remote.discovery.configured"));
    }

    public void testExportImport_ImportingDISCO() throws Exception {
        doExportImportTest(false,
            getBundle(getChildContext("CHILD2").getBundleContext(), "org.amdatu.remote.discovery.configured"));
    }

    /**
     * AMDATURS-83 : Test that previously exported services are properly propagated after a configuration
     * driven restart.
     */
    public void testBasicServiceExportImportRestart() throws Exception {

        BlockingRemoteServiceAdminListener importBlock =
            new BlockingRemoteServiceAdminListener(getChildContext("CHILD2").getBundleContext(),
                RemoteServiceAdminEvent.IMPORT_REGISTRATION, "(qqq=123)");
        importBlock.register();

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        properties.put("qqq", "123");

        ServiceRegistration<?> exportedServiceRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(EchoInterface.class.getName(),
                mock(EchoInterface.class), properties);

        try {
            if (!importBlock.await(5, TimeUnit.SECONDS)) {
                fail("No import event received");
            }
            ServiceReference<?>[] refs =
                getChildContext("CHILD2").getBundleContext().getAllServiceReferences(EchoInterface.class.getName(),
                    null);
            assertEquals(1, refs.length);

            getChildContext("CHILD1").configure("org.amdatu.remote.discovery.configured",
                "org.amdatu.remote.discovery.configured.path", "qqq124",
                "org.amdatu.remote.discovery.configured.endpoints", "http://localhost:" + httpPort2 + "/qqq421");

            getChildContext("CHILD2").configure("org.amdatu.remote.discovery.configured",
                "org.amdatu.remote.discovery.configured.path", "qqq421",
                "org.amdatu.remote.discovery.configured.endpoints", "http://localhost:" + httpPort1 + "/qqq124");

            importBlock.reset();
            if (!importBlock.await(5, TimeUnit.SECONDS)) {
                fail("No import event received");
            }
            refs =
                getChildContext("CHILD2").getBundleContext().getAllServiceReferences(EchoInterface.class.getName(),
                    null);
            assertEquals(1, refs.length);
        }
        finally {
            importBlock.unregister();
            exportedServiceRegistration.unregister();
        }
    }
}
