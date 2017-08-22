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

import java.util.Enumeration;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.amdatu.remote.itest.config.Config;
import org.amdatu.remote.itest.config.FrameworkConfig;
import org.amdatu.remote.itest.util.FrameworkContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 *
 */
public class SLPEndpointDiscoveryFullIntegrationTest extends AbstractFullIntegrationTest {

    private String httpPort1;
    private String httpPort2;

    @Override
    protected Config[] configureFramework(final FrameworkContext parent) throws Exception {
        BundleContext parentBC = getParentContext().getBundleContext();

        String systemPackages = parentBC.getProperty("itest.systempackages");
        String defaultBundles = parentBC.getProperty("itest.bundles.default");
        String remoteServiceAdminBundles = parentBC.getProperty(m_remoteAdminPropertyKey);
        String topologyManagerBundles = parentBC.getProperty("itest.bundles.topology.promiscuous");
        String discoveryBundles = parentBC.getProperty("itest.bundles.discovery.slp");

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

        getChildContext("CHILD1").configure("org.amdatu.remote.discovery.slp",
            "org.amdatu.remote.discovery.slp.schedule", "2");

        getChildContext("CHILD2").configure("org.amdatu.remote.discovery.slp",
            "org.amdatu.remote.discovery.slp.schedule", "1");

        ConsoleHandler handler = new ConsoleHandler();
        Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            if (loggerName.startsWith("org.livetribe")) {
                Logger.getLogger(loggerName).addHandler(handler);
                Logger.getLogger(loggerName).setLevel(Level.FINEST);
            }
        }
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
            getBundle(getChildContext("CHILD1").getBundleContext(), "org.amdatu.remote.discovery.slp"));
    }

    // AMDATU-138 Restarting HTTP causes the Topology Manager component to restart
    public void testExportImport_HttpService() throws Exception {
        doExportImportTest(true,
            getBundle(getChildContext("CHILD1").getBundleContext(), "org.apache.felix.http.jetty"));
    }

    // TODO by Damiaan van der Kruk: this test fails when runned standalone, this has probably something to do with the timeout checks
    public void testExportImport_ImportingDISCO() throws Exception {
        doExportImportTest(false,
            getBundle(getChildContext("CHILD2").getBundleContext(), "org.amdatu.remote.discovery.slp"));
    }
}
