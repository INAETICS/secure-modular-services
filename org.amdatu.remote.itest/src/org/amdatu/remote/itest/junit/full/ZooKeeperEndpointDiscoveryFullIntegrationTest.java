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

import java.io.File;

import org.amdatu.remote.itest.config.Config;
import org.amdatu.remote.itest.config.FrameworkConfig;
import org.amdatu.remote.itest.util.FrameworkContext;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 *
 */
public class ZooKeeperEndpointDiscoveryFullIntegrationTest extends AbstractFullIntegrationTest {

    private String httpPort1;
    private String httpPort2;

    private ZooKeeperServer server;
    private ServerCnxnFactory factory;

    @Override
    protected Config[] configureFramework(final FrameworkContext parent) throws Exception {
        BundleContext parentBC = getParentContext().getBundleContext();

        String systemPackages = parentBC.getProperty("itest.systempackages");
        String defaultBundles = parentBC.getProperty("itest.bundles.default");
        String remoteServiceAdminBundles = parentBC.getProperty(m_remoteAdminPropertyKey);
        String topologyManagerBundles = parentBC.getProperty("itest.bundles.topology.promiscuous");
        String discoveryBundles = parentBC.getProperty("itest.bundles.discovery.zookeeper");

        parent.setLogLevel(LogService.LOG_DEBUG);
        parent.setServiceTimout(30000);

        httpPort1 = getRandomFreePort();
        httpPort2 = getRandomFreePort();

        FrameworkConfig child1 = frameworkConfig("CHILD1")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(30000)
            .frameworkProperty("felix.cm.loglevel", "4")
            .frameworkProperty("org.osgi.service.http.port", httpPort1)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .bundlePaths(defaultBundles, remoteServiceAdminBundles, topologyManagerBundles, discoveryBundles);

        FrameworkConfig child2 = frameworkConfig("CHILD2")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(30000)
            .frameworkProperty("felix.cm.loglevel", "4")
            .frameworkProperty("org.osgi.service.http.port", httpPort2)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .bundlePaths(defaultBundles, remoteServiceAdminBundles, topologyManagerBundles, discoveryBundles);

        return configs(child1, child2);
    }

    @Override
    protected void configureServices() throws Exception {

        // Run a simple zookeeper server in the parent context. Bundles are present.
        int port = 28811;
        File root = new File(getParentContext().getBundleContext().getDataFile(""), "zookeeper");
        root.mkdirs();
        server = new ZooKeeperServer(root, root, 2000);
        factory = ServerCnxnFactory.createFactory(port, 50);
        factory.startup(server);

        getChildContext("CHILD2").configure(m_RSABundleName, "org.amdatu.remote.admin.http.readtimeout",
            "1000");

        // Set connect strings so the clients connect to the freshly created server.
        getChildContext("CHILD1").configure("org.amdatu.remote.discovery.zookeeper",
            "org.amdatu.remote.discovery.zookeeper.connectstring", "0.0.0.0:" + port,
            "org.amdatu.remote.discovery.zookeeper.rootpath", "/discovery",
            "org.amdatu.remote.discovery.zookeeper.schedule", "2");

        getChildContext("CHILD2").configure("org.amdatu.remote.discovery.zookeeper",
            "org.amdatu.remote.discovery.zookeeper.connectstring", "0.0.0.0:" + port,
            "org.amdatu.remote.discovery.zookeeper.rootpath", "/discovery",
            "org.amdatu.remote.discovery.zookeeper.schedule", "3");
    }

    @Override
    protected void cleanupTest() throws Exception {

        // Reset connect strings so the clients disconnect before the server shuts down.
        getChildContext("CHILD1").configure("org.amdatu.remote.discovery.zookeeper",
            "org.amdatu.remote.discovery.zookeeper.connectstring", "",
            "org.amdatu.remote.discovery.zookeeper.rootpath", "/discovery");

        getChildContext("CHILD2").configure("org.amdatu.remote.discovery.zookeeper",
            "org.amdatu.remote.discovery.zookeeper.connectstring", "",
            "org.amdatu.remote.discovery.zookeeper.rootpath", "/discovery");

        // Clear zookeeper
        factory.shutdown();
        server.shutdown();
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
            getBundle(getChildContext("CHILD1").getBundleContext(), "org.amdatu.remote.discovery.zookeeper"));
    }

    public void testExportImport_ImportingDISCO() throws Exception {
        doExportImportTest(false,
            getBundle(getChildContext("CHILD2").getBundleContext(), "org.amdatu.remote.discovery.zookeeper"));
    }
}
