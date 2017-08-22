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

import org.amdatu.remote.itest.config.Config;
import org.amdatu.remote.itest.config.FrameworkConfig;
import org.amdatu.remote.itest.util.FrameworkContext;
import org.amdatu.remote.itest.util.ITestUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 *
 */
public class BonjourEndpointDiscoveryFullIntegrationTest extends AbstractFullIntegrationTest {

    private String httpPort1;
    private String httpPort2;

    @Override
    protected Config[] configureFramework(final FrameworkContext parent) throws Exception {
        BundleContext parentBC = getParentContext().getBundleContext();

        String systemPackages = parentBC.getProperty("itest.systempackages");
        String defaultBundles = parentBC.getProperty("itest.bundles.default");
        String remoteServiceAdminBundles = parentBC.getProperty(m_remoteAdminPropertyKey);
        String topologyManagerBundles = parentBC.getProperty("itest.bundles.topology.promiscuous");
        String discoveryBundles = parentBC.getProperty("itest.bundles.discovery.bonjour");

        parent.setLogLevel(LogService.LOG_DEBUG);
        parent.setServiceTimout(30000);

        // AMDATURS-104
        // Default binding IP for the discovery is 0.0.0.0, see the Activator.
        // This can lead to failing tests when e.g. VirtualBox is installed, because the discovery
        // tries to bind to the wrong network interface.
        // So try to get the real IP of a network interface we can use, and configure the
        // discovery to use that one
        String bindingIp = ITestUtil.getCurrentIpv4Address();

        httpPort1 = getRandomFreePort();
        httpPort2 = getRandomFreePort();

        FrameworkConfig child1 = frameworkConfig("CHILD1")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(30000)
            .frameworkProperty("felix.cm.loglevel", "1")
            .frameworkProperty("org.apache.felix.http.host", bindingIp)
            .frameworkProperty("org.osgi.service.http.port", httpPort1)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .bundlePaths(defaultBundles, remoteServiceAdminBundles, topologyManagerBundles, discoveryBundles);

        FrameworkConfig child2 = frameworkConfig("CHILD2")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(30000)
            .frameworkProperty("felix.cm.loglevel", "1")
            .frameworkProperty("org.apache.felix.http.host", bindingIp)
            .frameworkProperty("org.osgi.service.http.port", httpPort2)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .bundlePaths(defaultBundles, remoteServiceAdminBundles, topologyManagerBundles, discoveryBundles);

        return configs(child1, child2);
    }

    @Override
    protected void configureServices() throws Exception {
        getChildContext("CHILD2").configure(m_RSABundleName, "org.amdatu.remote.admin.http.readtimeout",
            "1000");

        getChildContext("CHILD1").configure("org.amdatu.remote.discovery.bonjour",
            "org.amdatu.remote.discovery.bonjour.schedule", "1");

        getChildContext("CHILD2").configure("org.amdatu.remote.discovery.bonjour",
            "org.amdatu.remote.discovery.bonjour.schedule", "2");
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
            getBundle(getChildContext("CHILD1").getBundleContext(), "org.amdatu.remote.discovery.bonjour"));
    }

    public void testExportImport_ImportingDISCO() throws Exception {
        doExportImportTest(false,
            getBundle(getChildContext("CHILD2").getBundleContext(), "org.amdatu.remote.discovery.bonjour"));
    }
}
