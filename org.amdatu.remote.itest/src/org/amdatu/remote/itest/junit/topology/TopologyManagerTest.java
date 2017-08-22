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
package org.amdatu.remote.itest.junit.topology;

import static org.amdatu.remote.itest.config.Configs.configs;
import static org.amdatu.remote.itest.config.Configs.frameworkConfig;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.amdatu.remote.admin.itest.api.EchoInterface;
import org.amdatu.remote.itest.config.Config;
import org.amdatu.remote.itest.config.FrameworkConfig;
import org.amdatu.remote.itest.junit.RemoteServicesTestBase;
import org.amdatu.remote.itest.util.FrameworkContext;
import org.amdatu.remote.topology.promiscuous.PromiscuousTopologyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Testing {@link PromiscuousTopologyManager} implementation in isolation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class TopologyManagerTest extends RemoteServicesTestBase {

    @Override
    protected Config[] configureFramework(FrameworkContext parent) throws Exception {

        String systemPackages =
            getParentContext().getBundleContext().getProperty("itest.systempackages");
        String defaultBundles =
            getParentContext().getBundleContext().getProperty("itest.bundles.default");
        String topologyManagerBundles =
            getParentContext().getBundleContext().getProperty("itest.bundles.topology.promiscuous");

        parent.setLogLevel(LogService.LOG_DEBUG);
        parent.setServiceTimout(30000);

        FrameworkConfig child1 = frameworkConfig("CHILD1")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(10000)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .bundlePaths(defaultBundles, topologyManagerBundles);

        return configs(child1);
    }

    @Override
    protected void configureServices() throws Exception {
    }

    @Override
    protected void cleanupTest() throws Exception {
    }

    /**
     * Test that combines several tests that can safely run sequentially in one framework.
     * This is just more efficient then starting a fresh framework for every test.
     * 
     * @throws Exception
     */
    public void testTopologyManager() throws Exception {
        assertTopologyManagerInvalidOK();
        assertTopologyManagerRaceOK();
        assertTopologyManagerCallsNewRSA();
        assertTopologyManagerExportsUpdate();
        assertTopologyManagerExportsUpdateWithInitialFilter();
        assertTopologyManagerServiceUpdate();
    }

    /**
     * AMDATURS-37 - Check that an Export Registration is properly closed when the Service Registration is
     * unregistered immediately after its export.
     */
    @SuppressWarnings("unchecked")
    public void assertTopologyManagerRaceOK() throws Exception {

        RemoteServiceAdmin remoteServiceAdmin = mock(RemoteServiceAdmin.class);
        ExportRegistration exportRegistration = mock(ExportRegistration.class);
        Collection<ExportRegistration> registrations = Collections.singletonList(exportRegistration);
        when(remoteServiceAdmin.exportService(notNull(ServiceReference.class), any(Map.class))).thenReturn(
            registrations);

        ServiceRegistration<?> rsaRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(RemoteServiceAdmin.class.getName(),
                remoteServiceAdmin, null);

        Dictionary<String, Object> localProperties = new Hashtable<String, Object>();
        localProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        ServiceRegistration<?> svcRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(EchoInterface.class.getName(),
                mock(EchoInterface.class), localProperties);
        ServiceReference<?> svcReference = svcRegistration.getReference();
        svcRegistration.unregister();

        try {
            verify(remoteServiceAdmin, timeout(30000)).exportService(svcReference, null);
            verify(exportRegistration, timeout(30000)).close();
        }
        finally {
            rsaRegistration.unregister();
        }
    }

    /**
     * AMDATURS-90 - Check that an Export Registration is properly closed when the it is
     * invalid and thus #getExportReference() throws an exception.
     */
    @SuppressWarnings("unchecked")
    public void assertTopologyManagerInvalidOK() throws Exception {

        RemoteServiceAdmin remoteServiceAdmin = mock(RemoteServiceAdmin.class);
        ExportRegistration exportRegistration = mock(ExportRegistration.class);
        Collection<ExportRegistration> registrations = Collections.singletonList(exportRegistration);
        when(exportRegistration.getExportReference()).thenThrow(IllegalStateException.class);
        when(exportRegistration.getException()).thenReturn(new IllegalStateException());
        when(remoteServiceAdmin.exportService(notNull(ServiceReference.class), any(Map.class))).thenReturn(
            registrations);

        ServiceRegistration<?> rsaRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(RemoteServiceAdmin.class.getName(),
                remoteServiceAdmin, null);

        Dictionary<String, Object> localProperties = new Hashtable<String, Object>();
        localProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        ServiceRegistration<?> svcRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(EchoInterface.class.getName(),
                mock(EchoInterface.class), localProperties);
        ServiceReference<?> svcReference = svcRegistration.getReference();
        svcRegistration.unregister();

        try {
            verify(remoteServiceAdmin, timeout(30000)).exportService(svcReference, null);
            verify(exportRegistration, timeout(30000)).close();
        }
        finally {
            rsaRegistration.unregister();
        }
    }

    /**
     * Tests that a topology manager calls a newly registered RSA to export services.
     */
    public void assertTopologyManagerCallsNewRSA() throws Exception {
        Dictionary<String, Object> localProperties = new Hashtable<String, Object>();
        localProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());

        EchoInterface localEchoService = mock(EchoInterface.class);

        ServiceRegistration<?> svcRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(EchoInterface.class.getName(),
                localEchoService,
                localProperties);
        ServiceReference<?> localServiceReference = svcRegistration.getReference();

        RemoteServiceAdmin remoteServiceAdmin = mock(RemoteServiceAdmin.class);
        ServiceRegistration<?> rsaRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(RemoteServiceAdmin.class.getName(),
                remoteServiceAdmin, null);

        try {
            verify(remoteServiceAdmin, timeout(30000)).exportService(localServiceReference, null);
        }
        finally {
            svcRegistration.unregister();
            rsaRegistration.unregister();
        }
    }

    @SuppressWarnings("unchecked")
    public void assertTopologyManagerExportsUpdate() throws Exception {

        RemoteServiceAdmin remoteServiceAdmin = mock(RemoteServiceAdmin.class);
        ExportRegistration exportRegistration = mock(ExportRegistration.class);
        Collection<ExportRegistration> registrations = Collections.singletonList(exportRegistration);
        when(remoteServiceAdmin.exportService(notNull(ServiceReference.class), any(Map.class))).thenReturn(
            registrations);

        ServiceRegistration<?> rsaRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(RemoteServiceAdmin.class.getName(),
                remoteServiceAdmin, null);

        Dictionary<String, Object> localProperties = new Hashtable<String, Object>();
        localProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        localProperties.put("a", "b");
        ServiceRegistration<?> svcRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(EchoInterface.class.getName(),
                mock(EchoInterface.class), localProperties);
        ServiceReference<?> svcReference = svcRegistration.getReference();

        try {

            // default should match
            verify(remoteServiceAdmin, timeout(30000).times(1)).exportService(svcReference, null);

            // non matching filter should close existing exports
            getChildContext("CHILD1").configure("org.amdatu.remote.topology.promiscuous",
                "org.amdatu.remote.topology.promiscuous.exports", "(a=c)");
            verify(exportRegistration, timeout(30000).times(1)).close();

            // matching filter should exports existing candidates
            getChildContext("CHILD1").configure("org.amdatu.remote.topology.promiscuous",
                "org.amdatu.remote.topology.promiscuous.exports", "(a=b)");

            verify(remoteServiceAdmin, timeout(30000).times(2)).exportService(svcReference, null);
        }
        finally {
            svcRegistration.unregister();
            rsaRegistration.unregister();
        }
    }

    @SuppressWarnings("unchecked")
    public void assertTopologyManagerExportsUpdateWithInitialFilter() throws Exception {

        RemoteServiceAdmin remoteServiceAdmin = mock(RemoteServiceAdmin.class);
        ExportRegistration exportRegistration = mock(ExportRegistration.class);
        Collection<ExportRegistration> registrations = Collections.singletonList(exportRegistration);
        when(remoteServiceAdmin.exportService(notNull(ServiceReference.class), any(Map.class))).thenReturn(
            registrations);

        BundleContext bundleContext = getChildContext("CHILD1").getBundleContext();

        ServiceRegistration<?> rsaRegistration =
            bundleContext.registerService(RemoteServiceAdmin.class.getName(), remoteServiceAdmin, null);

        // set initial filter
        getChildContext("CHILD1").configure("org.amdatu.remote.topology.promiscuous",
            "org.amdatu.remote.topology.promiscuous.exports", "(a=c)");

        Dictionary<String, Object> localProperties = new Hashtable<String, Object>();
        localProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        localProperties.put("a", "b");
        ServiceRegistration<?> svcRegistration =
            bundleContext.registerService(EchoInterface.class.getName(), mock(EchoInterface.class), localProperties);
        ServiceReference<?> svcReference = svcRegistration.getReference();

        try {

            // service should not be exported
            verify(remoteServiceAdmin, never()).exportService(svcReference, null);

            // update filter, service should be exported now
            getChildContext("CHILD1").configure("org.amdatu.remote.topology.promiscuous",
                "org.amdatu.remote.topology.promiscuous.exports", "(a=b)");

            verify(remoteServiceAdmin, timeout(30000).times(1)).exportService(svcReference, null);

        }
        finally {
            svcRegistration.unregister();
            rsaRegistration.unregister();
        }
    }

    @SuppressWarnings("unchecked")
    public void assertTopologyManagerServiceUpdate() throws Exception {

        RemoteServiceAdmin remoteServiceAdmin = mock(RemoteServiceAdmin.class);
        ExportRegistration exportRegistration = mock(ExportRegistration.class);
        Collection<ExportRegistration> registrations = Collections.singletonList(exportRegistration);
        when(remoteServiceAdmin.exportService(notNull(ServiceReference.class), any(Map.class))).thenReturn(
            registrations);

        ServiceRegistration<?> rsaRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(RemoteServiceAdmin.class.getName(),
                remoteServiceAdmin, null);

        Dictionary<String, Object> localProperties = new Hashtable<String, Object>();
        localProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        localProperties.put("a", "b");
        ServiceRegistration<?> svcRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(EchoInterface.class.getName(),
                mock(EchoInterface.class), localProperties);
        ServiceReference<?> svcReference = svcRegistration.getReference();

        try {

            // default should match
            verify(remoteServiceAdmin, timeout(30000).times(1)).exportService(svcReference, null);

            // matching filter should not do anything
            getChildContext("CHILD1").configure("org.amdatu.remote.topology.promiscuous",
                "org.amdatu.remote.topology.promiscuous.exports", "(a=b)");

            verify(exportRegistration, never()).close();
            verify(remoteServiceAdmin, times(1)).exportService(svcReference, null);

            // new service properties should remove export
            localProperties.put("a", "c");
            svcRegistration.setProperties(localProperties);
            verify(exportRegistration, timeout(30000).times(1)).close();

        }
        finally {
            svcRegistration.unregister();
            rsaRegistration.unregister();
        }
    }
}
