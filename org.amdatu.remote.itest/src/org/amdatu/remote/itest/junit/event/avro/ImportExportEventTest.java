/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.amdatu.remote.itest.junit.event.avro;

import org.amdatu.remote.admin.itest.api.EchoInterface;

public class ImportExportEventTest extends org.amdatu.remote.itest.junit.event.ImportExportEventTest {

    public ImportExportEventTest() {
        m_remoteAdminPropertyKey = "itest.bundles.admin.http.avro";
        m_config_type = org.amdatu.remote.admin.http.avro.HttpAdminConstants.CONFIGURATION_TYPE;
        m_endpoint_url_key = org.amdatu.remote.admin.http.avro.HttpAdminConstants.ENDPOINT_URL;
        m_interface_class = EchoInterface.class;
    }
}
