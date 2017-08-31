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
package org.amdatu.remote;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import org.amdatu.remote.discovery.HttpEndpointDiscoveryConfiguration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * 
 * @author Sudohenk
 *
 */
public final class SecureEndpointUtil {

    private final static SecureEndpointDescriptorReader m_reader = new SecureEndpointDescriptorReader();
    private final static SecureEndpointDescriptorWriter m_writer = new SecureEndpointDescriptorWriter();
    private final static EndpointHashGenerator m_hasher = new EndpointHashGenerator();

    private SecureEndpointUtil() {
    }

    public static List<EndpointDescription> readEndpoints(Reader reader, HttpEndpointDiscoveryConfiguration m_configuration) throws IOException {
        return m_reader.parseDocument(reader, m_configuration);
    }

    public static void writeEndpoints(Writer writer, HttpEndpointDiscoveryConfiguration m_configuration, EndpointDescription... endpoints) throws IOException {
        try {
            m_writer.writeDocument(writer, m_configuration, endpoints);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String computeHash(EndpointDescription endpoint) {
        return m_hasher.hash(endpoint.getProperties());
    }
}
