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
package org.amdatu.remote.admin.http.avro;

import java.net.URL;

public abstract class HttpAvroEndpointTestBase extends org.amdatu.remote.admin.http.HttpEndpointTestBase {

    protected HttpAdminConfiguration m_configuration;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        m_configuration = new HttpAdminConfiguration() {
            @Override
            public int getReadTimeout() {
                return 1000;
            }

            @Override
            public int getConnectTimeout() {
                return 1000;
            }

            @Override
            public URL getBaseUrl() {
                return m_endpointURL;
            }
        };

    }
}
