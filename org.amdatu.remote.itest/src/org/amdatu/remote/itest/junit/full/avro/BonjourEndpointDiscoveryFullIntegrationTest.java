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
package org.amdatu.remote.itest.junit.full.avro;

/**
 * Class for testing Bonjour integration test with RSA HTTP Avro.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 *
 */
public class BonjourEndpointDiscoveryFullIntegrationTest
    extends org.amdatu.remote.itest.junit.full.BonjourEndpointDiscoveryFullIntegrationTest {

    public BonjourEndpointDiscoveryFullIntegrationTest() {
        m_config_type = org.amdatu.remote.admin.http.avro.HttpAdminConstants.CONFIGURATION_TYPE;
        m_remoteAdminPropertyKey = "itest.bundles.admin.http.avro";
        m_RSABundleName = "org.amdatu.remote.admin.http.avro";
    }
}
