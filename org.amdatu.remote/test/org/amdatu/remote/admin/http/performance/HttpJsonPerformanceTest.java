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
package org.amdatu.remote.admin.http.performance;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;

import org.amdatu.remote.admin.http.HttpAdminConfiguration;
import org.amdatu.remote.admin.http.HttpClientEndpoint;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpJsonPerformanceTest extends HttpPerformanceTestBase {

    final ObjectMapper m_mapper = new ObjectMapper();

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

    @Override
    protected byte[] getFlipRespnse(final String result) {
        String json = "{ \"r\" : \"" + m_bigStrFlipped + "\"}";
        return json.getBytes();
    }

    @Override
    protected <T> T getProxy(final Class<T> intrfc) {
        return new HttpClientEndpoint(m_endpointURL, m_configuration, intrfc).getServiceProxy();
    }

    @Override
    protected byte[] getPoisRespnse(final List<POI> result) {
        final StringWriter sw = new StringWriter();
        try {
            m_mapper.writeValue(sw, result);
            sw.close();
            String json = "{ \"r\" : " + sw.toString() + "}";
            return json.getBytes();
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        throw new IllegalStateException();
    }

    @Override
    protected String getType() {
        return "Json";
    }

    @Override
    protected byte[] getSomeFloatResponse(final float result) {
        final StringWriter sw = new StringWriter();
        try {
            m_mapper.writeValue(sw, result);
            sw.close();
            String json = "{ \"r\" : " + sw.toString() + "}";
            return json.getBytes();
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        throw new IllegalStateException();
    }

    @Override
    protected byte[] getTestString(final String result) {
        String json = "{ \"r\" : \"" + result + "\"}";
        return json.getBytes();
    }

}
