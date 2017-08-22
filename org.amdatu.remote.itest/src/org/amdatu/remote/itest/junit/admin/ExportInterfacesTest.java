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
package org.amdatu.remote.itest.junit.admin;

import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;

import java.util.Dictionary;
import java.util.Hashtable;

import org.amdatu.remote.admin.itest.api.EchoImpl;
import org.amdatu.remote.admin.itest.api.EchoInterface;
import org.amdatu.remote.admin.itest.api.ExtendedEchoImpl;
import org.amdatu.remote.admin.itest.api.ExtendedEchoInterface;

/**
 * Tests the exporting of interface functionality.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class ExportInterfacesTest extends AbstractRemoteServiceAdminTest {

    /**
     * Remote Service Admin 122.5.1
     * <br/><br/>
     * service.exported.interfaces – (String+) This property must be set; it marks this service for export and defines the
     * interfaces.<br/>
     * The list members must all be contained in the types listed in the objectClass service property from the Service
     * Reference. The single value of an asterisk (’*’, \u002A) indicates all interfaces in the registration’s objectClass
     * property and ignore the classes. Being able to set this property outside the Service Reference implies that the
     * Topology Manager can export any registered service, also services not specifically marked to be exported.
     */
    public void testRsaHandlesExportInterfaces() throws Exception {
        doTestRsaHandlesExportedInterfaceWithoutExportPropertyFail();
        doTestRsaHandlesExportedInterfaceWithNonImplementedInterfaceFail();
        doTestRsaHandlesWildcardMixedWithExportedInterfaceFail();

        doTestRsaHandlesExportedInterfaceOk();
        doTestRsaHandlesWildcardAsExportedInterfaceOk();
        doTestRsaHandlesMultipleExportedInterfacesOk();
        doTestRsaHandlesExportedInterfaceWithSubTypeServiceOk();
    }

    /**
     * Tests that exporting a simple service works as expected.
     */
    protected void doTestRsaHandlesExportedInterfaceOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());

        assertExportRegistrationSucceeds(serviceProperties, null, null, null, createEchoInstance(),
            EchoInterface.class);
    }

    /**
     * Tests that we cannot export a service that does not implement an exported interface.
     */
    protected void doTestRsaHandlesExportedInterfaceWithNonImplementedInterfaceFail() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, ExtendedEchoInterface.class.getName());

        // AMDATURS-121
        assertExportRegistrationFails(serviceProperties, null, IllegalArgumentException.class, createEchoInstance());
    }

    /**
     * Tests that trying to export a service without at least one exported interface fails with an exception.
     */
    protected void doTestRsaHandlesExportedInterfaceWithoutExportPropertyFail() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();

        assertExportRegistrationFails(serviceProperties, null, IllegalArgumentException.class, createEchoInstance());
    }

    /**
     * Tests that trying to export a service without at least one exported interface fails with an exception.
     */
    protected void doTestRsaHandlesExportedInterfaceWithEmptyExportPropertyFail() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, new String[0]);

        assertExportRegistrationFails(serviceProperties, null, IllegalArgumentException.class, createEchoInstance());
    }

    /**
     * Tests that we can export a service that implements a subtype of the exported interface.
     */
    protected void doTestRsaHandlesExportedInterfaceWithSubTypeServiceOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());

        assertExportRegistrationSucceeds(serviceProperties, null, null, null, createExtendedEchoInstance(),
            EchoInterface.class);
    }

    /**
     * Tests that we can export a service that implements multiple exported interface.
     */
    protected void doTestRsaHandlesMultipleExportedInterfacesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES,
            strings(EchoInterface.class.getName(), ExtendedEchoInterface.class.getName()));

        assertExportRegistrationSucceeds(serviceProperties, null, null, null, createExtendedEchoInstance(),
            EchoInterface.class, ExtendedEchoInterface.class);
    }

    /**
     * Tests that exporting all interfaces implemented by a service by using a wildcard works as expected.
     */
    protected void doTestRsaHandlesWildcardAsExportedInterfaceOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, "*");

        assertExportRegistrationSucceeds(serviceProperties, null, null, null, createExtendedEchoInstance(),
            EchoInterface.class, ExtendedEchoInterface.class);
    }

    /**
     * Tests that exporting a wildcard as exported interface can not occur together with other interface names.
     */
    protected void doTestRsaHandlesWildcardMixedWithExportedInterfaceFail() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES,
            strings(EchoInterface.class.getName(), "*"));

        assertExportRegistrationFails(serviceProperties, null, IllegalArgumentException.class,
            createExtendedEchoInstance());
    }

    private Object createEchoInstance() throws Exception {
        return createInstance(EchoImpl.class.getName());
    }

    private Object createExtendedEchoInstance() throws Exception {
        return createInstance(ExtendedEchoImpl.class.getName());
    }
}
