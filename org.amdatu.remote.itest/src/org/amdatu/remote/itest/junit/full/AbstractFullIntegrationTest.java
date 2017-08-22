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

import static org.amdatu.remote.ServiceUtil.getStringPlusValue;
import static org.amdatu.remote.itest.util.ITestUtil.stringArrayEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_REGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_UNREGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_UPDATE;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_REGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_UNREGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_UPDATE;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.amdatu.remote.admin.itest.api.EchoData;
import org.amdatu.remote.admin.itest.api.EchoInterface;
import org.amdatu.remote.itest.junit.RemoteServicesTestBase;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * Abstract test for full integration with an RemoteServiceAdmin, a TopologyManager and a Discovery
 * implementation. Concrete implementation must provision from frameworks.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractFullIntegrationTest extends RemoteServicesTestBase {

    protected String m_config_type = org.amdatu.remote.admin.http.HttpAdminConstants.CONFIGURATION_TYPE;
    protected String m_remoteAdminPropertyKey = "itest.bundles.admin.http";
    protected String m_RSABundleName = "org.amdatu.remote.admin.http";

    private static class IsRemoteServiceAdminEvent extends ArgumentMatcher<RemoteServiceAdminEvent> {

        private ExportReference m_exportRef;
        private int m_type;

        public IsRemoteServiceAdminEvent(final int type) {
            m_type = type;
        }

        @Override
        public boolean matches(final Object event) {
            RemoteServiceAdminEvent rsaEvent = (RemoteServiceAdminEvent) event;
            if (rsaEvent.getType() == EXPORT_REGISTRATION) {
                m_exportRef = rsaEvent.getExportReference();
            }
            return rsaEvent.getType() == m_type;
        }

        public ExportReference getExportReference() {
            return m_exportRef;
        }
    }

    /**
     * Main full integration tests
     */
    protected void doExportImportTest(final boolean restartAffectsExport, final Bundle... restartBundles)
        throws Exception {

        RemoteServiceAdminListener exportListener = mock(RemoteServiceAdminListener.class);
        ServiceRegistration<?> exportListenerReg =
            getChildContext("CHILD1").getBundleContext().registerService(RemoteServiceAdminListener.class,
                exportListener, null);

        RemoteServiceAdminListener importListener = mock(RemoteServiceAdminListener.class);
        ServiceRegistration<?> importListenerReg =
            getChildContext("CHILD2").getBundleContext().registerService(RemoteServiceAdminListener.class,
                importListener, null);

        ServiceRegistration<?> exportedServiceRegistration = null;
        try {

            logInfoHeader("Registering exported service");
            exportedServiceRegistration = registerExportedService();

            IsRemoteServiceAdminEvent exportRegistrationMatcher = new IsRemoteServiceAdminEvent(EXPORT_REGISTRATION);
            verify(exportListener, timeout(30000).times(1)).remoteAdminEvent(argThat(exportRegistrationMatcher));
            verify(importListener, timeout(30000).times(1)).remoteAdminEvent(
                argThat(new IsRemoteServiceAdminEvent(IMPORT_REGISTRATION)));

            EndpointDescription exportedEndpointDescription =
                exportRegistrationMatcher.getExportReference().getExportedEndpoint();
            assertNotNull(exportedEndpointDescription);

            logInfoHeader("Checking imported service");
            checkImportedService((long) exportedServiceRegistration.getReference().getProperty(Constants.SERVICE_ID),
                exportedEndpointDescription.getId(), false);

            logInfoHeader("Invoking imported service");
            invokeImportedService(false);

            logInfoHeader("Updating exported service");
            updateExportedService(exportedServiceRegistration);

            verify(exportListener, timeout(30000).times(1)).remoteAdminEvent(
                argThat(new IsRemoteServiceAdminEvent(EXPORT_UPDATE)));
            verify(importListener, timeout(30000).times(1)).remoteAdminEvent(
                argThat(new IsRemoteServiceAdminEvent(IMPORT_UPDATE)));

            logInfoHeader("Checking imported service");
            checkImportedService((long) exportedServiceRegistration.getReference().getProperty(Constants.SERVICE_ID),
                exportedEndpointDescription.getId(), true);

            logInfoHeader("Invoking imported service");
            invokeImportedService(false);

            logInfoHeader("Stopping restart bundle(s)");
            for (Bundle bundle : restartBundles) {
                logInfoLine(bundle.getSymbolicName() + "/" + bundle.getVersion());
                bundle.stop();
            }

            if (restartAffectsExport) {
                verify(exportListener, timeout(30000).times(1)).remoteAdminEvent(
                    argThat(new IsRemoteServiceAdminEvent(EXPORT_UNREGISTRATION)));
            }
            verify(importListener, timeout(30000).times(1)).remoteAdminEvent(
                argThat(new IsRemoteServiceAdminEvent(IMPORT_UNREGISTRATION)));

            logInfoHeader("Starting restart bundle(s)");
            for (Bundle bundle : restartBundles) {
                logInfoHeader(bundle.getSymbolicName());
                bundle.start();
            }
            Thread.sleep(1000);

            if (restartAffectsExport) {
                // Using atLeast because config updates may cause restarts
                // of the components, leading to multiple events...
                verify(exportListener, timeout(30000).atLeast(2)).remoteAdminEvent(argThat(exportRegistrationMatcher));
            }
            // Using atLeast because config updates may cause restarts
            // of the components, leading to multiple events...
            verify(importListener, timeout(30000).atLeast(2)).remoteAdminEvent(
                argThat(new IsRemoteServiceAdminEvent(IMPORT_REGISTRATION)));

            exportedEndpointDescription = exportRegistrationMatcher.getExportReference().getExportedEndpoint();
            assertNotNull(exportedEndpointDescription);

            logInfoHeader("Checking imported service");
            checkImportedService((long) exportedServiceRegistration.getReference().getProperty(Constants.SERVICE_ID),
                exportedEndpointDescription.getId(), false);

            logInfoHeader("Invoking imported service");
            invokeImportedService(true);

        }
        finally {
            exportListenerReg.unregister();
            importListenerReg.unregister();
            exportedServiceRegistration.unregister();
        }
    }

    protected void logInfoHeader(final String line) {
        getParentContext().getLogService().log(LogService.LOG_INFO, "-----------------------------------");
        getParentContext().getLogService().log(LogService.LOG_INFO, line);
        getParentContext().getLogService().log(LogService.LOG_INFO, "-----------------------------------");
    }

    protected void logInfoLine(final String line) {
        getParentContext().getLogService().log(LogService.LOG_INFO, line);
    }

    protected Bundle getBundle(final BundleContext context, final String bsn) {
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().equals(bsn)) {
                return bundle;
            }
        }
        return null;
    }

    private ServiceRegistration<?> registerExportedService() throws Exception {
        Dictionary<String, Object> exportedServiceProperties = new Hashtable<String, Object>();
        exportedServiceProperties.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        exportedServiceProperties
            .put("endpoint.package.version." + EchoInterface.class.getPackage().getName(), "1.0.0");
        exportedServiceProperties.put(SERVICE_EXPORTED_CONFIGS, m_config_type);
        exportedServiceProperties.put(SERVICE_INTENTS, new String[] { "passByValue" });
        exportedServiceProperties.put("arbitrary.prop", "bla");
        exportedServiceProperties.put(".private.prop", "bla");

        EchoInterface exportedEchoService = mock(EchoInterface.class);
        EchoData data = new EchoData(0, "nul");
        List<EchoData> dataList = new LinkedList<>();
        dataList.add(new EchoData(1, "een"));
        dataList.add(new EchoData(2, "twee"));

        when(exportedEchoService.echo("Amdatu")).thenReturn("Amdatu");
        when(exportedEchoService.echo1(Matchers.<EchoData> any())).thenReturn(data);
        when(exportedEchoService.echo2(Matchers.<List<EchoData>> any())).thenReturn(dataList);
        when(exportedEchoService.echo("Timeout")).thenAnswer(new Answer<String>() {
            @Override
            public String answer(final InvocationOnMock invocation) {
                try {
                    TimeUnit.SECONDS.sleep(30);
                }
                catch (Exception e) {}
                return "Hello Amdatu";
            }
        });

        return getChildContext("CHILD1").getBundleContext().registerService(EchoInterface.class.getName(),
            exportedEchoService, exportedServiceProperties);
    }

    private void updateExportedService(final ServiceRegistration<?> exportedServiceRegistration) throws Exception {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        for (String propertyKey : exportedServiceRegistration.getReference().getPropertyKeys()) {
            props.put(propertyKey, exportedServiceRegistration.getReference().getProperty(propertyKey));
        }
        props.put("someNewProperty", "aValue");
        exportedServiceRegistration.setProperties(props);
    }

    private void checkImportedService(final long exportedServiceId, final String exportedEndpointId,
        final boolean updated) throws Exception {

        getParentContext().getLogService().log(LogService.LOG_INFO, "Checking updated service import");

        ServiceReference<EchoInterface> importedServiceReference =
            getChildContext("CHILD2").getServiceReference(EchoInterface.class);

        Collection<ServiceReference<EchoInterface>> serviceReferences =
            getChildContext("CHILD2").getBundleContext().getServiceReferences(EchoInterface.class, null);
        assertEquals("Expected one service reference", 1, serviceReferences.size());

        assertTrue("Imported Service Property " + OBJECTCLASS + " must match Endpoint Description",
            stringArrayEquals(new String[] { EchoInterface.class.getName() },
                getStringPlusValue(importedServiceReference.getProperty(OBJECTCLASS))));

        assertEquals("Imported Service Property " + ENDPOINT_ID + " must be set correctly",
            exportedEndpointId,
            importedServiceReference.getProperty(ENDPOINT_ID));

        assertEquals("Imported Service Property " + ENDPOINT_SERVICE_ID + " must be set correctly",
            exportedServiceId,
            importedServiceReference.getProperty(ENDPOINT_SERVICE_ID));

        assertEquals(
            "Imported Service Property endpoint.package." + EchoInterface.class.getPackage().getName()
                + " must be set correctly",
            "1.0.0",
            importedServiceReference.getProperty("endpoint.package.version."
                + EchoInterface.class.getPackage().getName()));

        assertNotNull("Imported Service Property " + SERVICE_IMPORTED + " must be set to any value",
            importedServiceReference.getProperty(SERVICE_IMPORTED));

        assertTrue("Imported Service Property " + SERVICE_INTENTS + " must match Endpoint Description",
            stringArrayEquals(new String[] { "passByValue" },
                getStringPlusValue(importedServiceReference.getProperty(SERVICE_INTENTS))));

        assertTrue("Imported Service Property " + SERVICE_IMPORTED_CONFIGS + " must match Endpoint Description",
            stringArrayEquals(new String[] { m_config_type },
                getStringPlusValue(importedServiceReference.getProperty(SERVICE_IMPORTED_CONFIGS))));

        assertEquals("Imported Service Property arbitrary.prop must be set correctly", "bla",
            importedServiceReference.getProperty("arbitrary.prop"));

        assertNull("Imported Service Property .private.prop must not be set",
            importedServiceReference.getProperty(".private.prop"));

        if (updated) {
            assertEquals("Imported Service Property someNewProperty must be set correctly", "aValue",
                importedServiceReference.getProperty("someNewProperty"));
        }
    }

    private void invokeImportedService(final boolean includeTimeout) throws Exception {

        EchoInterface importedService = getChildContext("CHILD2").getService(EchoInterface.class);
        assertNotNull("No imported for service found!", importedService);
        assertEquals("Amdatu", importedService.echo("Amdatu"));

        EchoData data = new EchoData(0, "nul");

        EchoData result = importedService.echo1(data);
        assertEquals(data.getX(), result.getX());
        assertEquals(data.getY(), result.getY());

        List<EchoData> dataList = new LinkedList<>();
        dataList.add(new EchoData(1, "een"));
        dataList.add(new EchoData(2, "twee"));

        List<EchoData> resultList = importedService.echo2(dataList);
        assertEquals(dataList.get(0).getX(), resultList.get(0).getX());
        assertEquals(dataList.get(0).getY(), resultList.get(0).getY());
        assertEquals(dataList.get(1).getX(), resultList.get(1).getX());
        assertEquals(dataList.get(1).getY(), resultList.get(1).getY());

        if (includeTimeout) {
            try {
                String out = importedService.echo("Timeout");
                fail("expected timeout, got: " + out);
            }
            catch (Exception e) {
                // expected
            }
        }
    }
}
