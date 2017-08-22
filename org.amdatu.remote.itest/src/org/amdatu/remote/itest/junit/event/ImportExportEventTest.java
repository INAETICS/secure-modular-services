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

import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_REGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_UNREGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_UPDATE;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_ERROR;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_REGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_UNREGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_UPDATE;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_WARNING;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.amdatu.remote.ServiceUtil;
import org.amdatu.remote.admin.itest.api.EchoImpl;
import org.amdatu.remote.admin.itest.api.EchoInterface;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * Test for testing events fired during export and import of services, see
 * {@link RemoteServiceAdminListener}.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class ImportExportEventTest extends AbstractEventTest {

    protected String m_config_type = org.amdatu.remote.admin.http.HttpAdminConstants.CONFIGURATION_TYPE;
    protected String m_endpoint_url_key = org.amdatu.remote.admin.http.HttpAdminConstants.ENDPOINT_URL;
    protected Class m_interface_class = EchoInterface.class;

    /**
     * Tests that various events are fired for each import and export.
     */
    public void testEndpointPublishingComponent() throws Exception {
        doTestExportRegistrationEvents();
        doTestImportRegistrationEvents();
        doTestImportRegistrationErrorWarningEvents();
    }

    /**
     * Tests that exports of services causes the correct export-events to be
     * fired.
     */
    protected void doTestExportRegistrationEvents() throws Exception {
        final CountDownLatch registrationEventLatch = new CountDownLatch(1);
        final CountDownLatch updateEventLatch = new CountDownLatch(1);
        final CountDownLatch unregistrationEventLatch = new CountDownLatch(1);

        RemoteServiceAdminListener listener = new RemoteServiceAdminListener() {
            @Override
            public void remoteAdminEvent(final RemoteServiceAdminEvent event) {
                int type = event.getType();
                if (type == EXPORT_REGISTRATION) {
                    registrationEventLatch.countDown();
                }
                else if (type == EXPORT_UPDATE) {
                    updateEventLatch.countDown();
                }
                else if (type == EXPORT_UNREGISTRATION) {
                    unregistrationEventLatch.countDown();
                }
                else {
                    fail("Unexpected event type: " + type);
                }
            }
        };

        BundleContext childBC = getChildBundleContext();

        // Register our event listener...
        ServiceRegistration<?> listenerReg = childBC.registerService(RemoteServiceAdminListener.class, listener, null);

        String[] ifaces = { m_interface_class.getName() };

        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, ifaces);
        serviceProperties.put(SERVICE_EXPORTED_CONFIGS, m_config_type);

        ServiceRegistration<?> serviceReg = childBC.registerService(ifaces, new EchoImpl(), serviceProperties);

        Collection<ExportRegistration> exportRegistrations = null;

        try {
            exportRegistrations = m_remoteServiceAdmin.exportService(serviceReg.getReference(), null);

            assertTrue("Expected one or more export registration", exportRegistrations.size() > 0);

            // verify the EXPORT_REGISTRATION event...
            assertTrue(registrationEventLatch.await(5, TimeUnit.SECONDS));

            // Update the export properties...
            Map<String, String> newProps = Collections.singletonMap("some.property", "some.value");

            for (ExportRegistration exportRegistration : exportRegistrations) {
                exportRegistration.update(newProps);
            }

            // verify the EXPORT_UPDATE event...
            assertTrue(updateEventLatch.await(5, TimeUnit.SECONDS));

            for (ExportRegistration exportRegistration : exportRegistrations) {
                exportRegistration.close();
            }
            exportRegistrations = null;
            assertTrue(unregistrationEventLatch.await(5, TimeUnit.SECONDS));
        }
        finally {
            if (exportRegistrations != null) {
                for (ExportRegistration exportRegistration : exportRegistrations) {
                    exportRegistration.close();
                }
            }
            serviceReg.unregister();
            listenerReg.unregister();
        }
    }

    /**
     * Tests that warnings and errors for an imported service cause proper
     * import-evenst to be fired.
     */
    protected void doTestImportRegistrationErrorWarningEvents() throws Exception {
        final CountDownLatch registrationEventLatch = new CountDownLatch(1);
        final CountDownLatch unregistrationEventLatch = new CountDownLatch(1);
        final CountDownLatch warningEventLatch = new CountDownLatch(5);
        final CountDownLatch errorEventLatch = new CountDownLatch(1);

        RemoteServiceAdminListener listener = new RemoteServiceAdminListener() {
            @Override
            public void remoteAdminEvent(final RemoteServiceAdminEvent event) {
                int type = event.getType();
                if (type == IMPORT_REGISTRATION) {
                    registrationEventLatch.countDown();
                }
                else if (type == IMPORT_UPDATE) {
                    // Not tested here...
                }
                else if (type == IMPORT_UNREGISTRATION) {
                    unregistrationEventLatch.countDown();
                }
                else if (type == IMPORT_WARNING) {
                    warningEventLatch.countDown();
                }
                else if (type == IMPORT_ERROR) {
                    errorEventLatch.countDown();
                }
                else {
                    fail("Unexpected event type: " + type);
                }
            }
        };

        BundleContext childBC = getChildBundleContext();

        // Register our event listener...
        ServiceRegistration<?> listenerReg = childBC.registerService(RemoteServiceAdminListener.class, listener, null);

        Map<String, Object> properties = createEndpointProperties("bogusFrameworkUUID");
        EndpointDescription endpoint = new EndpointDescription(properties);

        ImportRegistration importRegistration = null;

        try {
            importRegistration = m_remoteServiceAdmin.importService(endpoint);

            assertNotNull(importRegistration);

            // verify the IMPORT_REGISTRATION event...
            assertTrue(registrationEventLatch.await(5, TimeUnit.SECONDS));

            // Call the actual service, should cause warning & failures...
            ServiceReference<?> serviceRef = importRegistration.getImportReference().getImportedService();

            EchoInterface service = (EchoInterface) childBC.getService(serviceRef);
            assertNotNull(service);

            // Try several times to call our service, which should -because it
            // is not
            // a real service- cause warnings and eventually an error event to
            // be fired...
            for (int i = 0; i < 6; i++) {
                try {
                    service.echo("name");
                }
                catch (Exception e) {
                    // Ignore, we're stubbornly retrying...
                }
            }

            // verify the IMPORT_WARNING event...
            assertTrue(warningEventLatch.await(5, TimeUnit.SECONDS));
            // verify the IMPORT_ERROR event...
            assertTrue(errorEventLatch.await(5, TimeUnit.SECONDS));

            // verify the IMPORT_UNREGISTRATION event...
            // assertTrue(unregistrationEventLatch.await(5, TimeUnit.SECONDS));
            // // XXX does not work yet, itest not correct yet!

            importRegistration.close();
            importRegistration = null;
            assertTrue(unregistrationEventLatch.await(5, TimeUnit.SECONDS));
        }
        finally {
            if (importRegistration != null) {
                importRegistration.close();
            }
            listenerReg.unregister();
        }
    }

    /**
     * Tests that imports of services causes the correct import-events to be
     * fired.
     */
    protected void doTestImportRegistrationEvents() throws Exception {
        final CountDownLatch registrationEventLatch = new CountDownLatch(1);
        final CountDownLatch updateEventLatch = new CountDownLatch(1);
        final CountDownLatch unregistrationEventLatch = new CountDownLatch(1);

        RemoteServiceAdminListener listener = new RemoteServiceAdminListener() {
            @Override
            public void remoteAdminEvent(final RemoteServiceAdminEvent event) {
                int type = event.getType();
                if (type == IMPORT_REGISTRATION) {
                    registrationEventLatch.countDown();
                }
                else if (type == IMPORT_UPDATE) {
                    updateEventLatch.countDown();
                }
                else if (type == IMPORT_UNREGISTRATION) {
                    unregistrationEventLatch.countDown();
                }
                else {
                    fail("Unexpected event type: " + type);
                }
            }
        };

        BundleContext childBC = getChildBundleContext();

        // Register our event listener...
        ServiceRegistration<?> listenerReg = childBC.registerService(RemoteServiceAdminListener.class, listener, null);

        Map<String, Object> properties = createEndpointProperties(ServiceUtil.getFrameworkUUID(childBC));
        EndpointDescription endpoint = new EndpointDescription(properties);

        ImportRegistration importRegistration = null;
        try {
            importRegistration = m_remoteServiceAdmin.importService(endpoint);

            assertNotNull(importRegistration);

            // verify the IMPORT_REGISTRATION event...
            assertTrue(registrationEventLatch.await(5, TimeUnit.SECONDS));

            // Update some of the properties of our endpoint...
            properties.put(m_endpoint_url_key, "http://localhost:9001");

            importRegistration.update(new EndpointDescription(properties));

            // verify the IMPORT_UPDATE event...
            assertTrue(updateEventLatch.await(5, TimeUnit.SECONDS));

            importRegistration.close();
            importRegistration = null;
            assertTrue(unregistrationEventLatch.await(5, TimeUnit.SECONDS));
        }
        finally {
            if (importRegistration != null) {
                importRegistration.close();
            }
            listenerReg.unregister();
        }

    }

    private Map<String, Object> createEndpointProperties(final String frameworkUUID) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(OBJECTCLASS, new String[] { m_interface_class.getName() });
        properties.put(ENDPOINT_ID, "123");
        properties.put(ENDPOINT_SERVICE_ID, 321l);
        properties.put(ENDPOINT_FRAMEWORK_UUID, frameworkUUID);
        properties.put("endpoint.package." + m_interface_class.getPackage().getName(), "1.0");
        properties.put(SERVICE_IMPORTED_CONFIGS, m_config_type);
        properties.put(SERVICE_INTENTS, new String[] { "passByValue" });
        properties.put(m_endpoint_url_key, "http://localhost:9000");
        return properties;
    }
}
