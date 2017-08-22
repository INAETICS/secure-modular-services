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
package org.amdatu.remote.itest.junit.admin.avro;

import static org.amdatu.remote.admin.http.HttpAdminConstants.PASSBYVALYE_INTENT;
import static org.mockito.Mockito.mock;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.amdatu.remote.admin.itest.api.EchoInterface;

/**
 * Tests the export of intents works correctly.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("restriction")
public class ExportIntentsTest extends AbstractRemoteServiceAdminTest {

    /**
     * Remote Service Admin 122.5.1
     * <br/><br/>
     * service.exported.intents – ( String+) A list of intents that the Remote Service Admin service must
     * implement to distribute the given service.<br/>
     * service.exported.intents.extra – (String+) This property is merged with the service.exported.intents
     * property.
     */
    public void testRsaHandlesExportedIntents() throws Exception {
        doTestRsaHandlesExportedIntentsWithoutExtraServicePropertiesOk();
        doTestRsaHandlesExportedIntentsWithEmptyServicePropertiesOk();
        doTestRsaHandlesExportedIntentsThroughExtraServicePropertiesOk();
        doTestRsaHandlesExportedIntentsThroughAdditionalIntentsOk();
        doTestRsaHandlesExportedIntentsExtraServicePropertiesOverrideServicePropertiesOk();
        doTestRsaHandlesExportedIntentsExtraIntentsOverrideServicePropertiesOk();

        doTestRsaHandlesUnsupportedExportedIntentsFails();
        doTestRsaHandlesUnsupportedMultipleIntentsFails();
        doTestRsaHandlesUnsupportedOverriddenExtraIntentsFails();
    }

    protected void doTestRsaHandlesExportedIntentsExtraIntentsOverrideServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS_EXTRA, "NotSupportedByRSA");

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_INTENTS_EXTRA, PASSBYVALYE_INTENT);

        assertExportRegistrationSucceeds(serviceProperties, extraProperties, null,
            strings(PASSBYVALYE_INTENT), mock(EchoInterface.class),
            EchoInterface.class);
    }

    protected void doTestRsaHandlesExportedIntentsExtraServicePropertiesOverrideServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS, "NotSupportedByRSA");

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_INTENTS, PASSBYVALYE_INTENT);

        assertExportRegistrationSucceeds(serviceProperties, extraProperties, null,
            strings(PASSBYVALYE_INTENT), mock(EchoInterface.class),
            EchoInterface.class);
    }

    protected void doTestRsaHandlesExportedIntentsThroughAdditionalIntentsOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_INTENTS_EXTRA, PASSBYVALYE_INTENT);

        assertExportRegistrationSucceeds(serviceProperties, extraProperties, null, strings(PASSBYVALYE_INTENT),
            mock(EchoInterface.class), EchoInterface.class);
    }

    protected void doTestRsaHandlesExportedIntentsThroughExtraServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_INTENTS, PASSBYVALYE_INTENT);

        assertExportRegistrationSucceeds(serviceProperties, extraProperties, null, strings(PASSBYVALYE_INTENT),
            mock(EchoInterface.class), EchoInterface.class);
    }

    protected void doTestRsaHandlesExportedIntentsWithEmptyServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS_EXTRA, PASSBYVALYE_INTENT);

        assertExportRegistrationSucceeds(serviceProperties, new HashMap<String, Object>(), null,
            strings(PASSBYVALYE_INTENT), mock(EchoInterface.class),
            EchoInterface.class);
    }

    protected void doTestRsaHandlesExportedIntentsWithoutExtraServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS, PASSBYVALYE_INTENT);
        serviceProperties.put("QQQ", "123");
        serviceProperties.put(".QQQ", "123");

        assertExportRegistrationSucceeds(serviceProperties, null, null, strings(PASSBYVALYE_INTENT),
            mock(EchoInterface.class), EchoInterface.class);
    }

    protected void doTestRsaHandlesUnsupportedExportedIntentsFails() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS, "NotSupportedByRSA");

        assertExportRegistrationFails(serviceProperties, null, null, mock(EchoInterface.class),
            EchoInterface.class);
    }

    protected void doTestRsaHandlesUnsupportedMultipleIntentsFails() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS, new String[] { "NotSupportedByRSA", PASSBYVALYE_INTENT });

        assertExportRegistrationFails(serviceProperties, null, null, mock(EchoInterface.class),
            EchoInterface.class);
    }

    protected void doTestRsaHandlesUnsupportedOverriddenExtraIntentsFails() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS, PASSBYVALYE_INTENT);

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_INTENTS_EXTRA, "NotSupportedByRSA");

        assertExportRegistrationFails(serviceProperties, extraProperties, null,
            mock(EchoInterface.class), EchoInterface.class);
    }
}
