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
package org.amdatu.remote.admin.http.avro;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.concurrent.atomic.AtomicReference;

import org.amdatu.remote.admin.http.TestUtil.AvroUnionType;
import org.amdatu.remote.admin.http.TestUtil.ServiceA;
import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.reflect.ReflectData;
import org.osgi.framework.ServiceException;

/**
 * Test cases for {@link HttpAvroClientEndpoint}.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class HttpAvroClientEndpointTest extends HttpAvroEndpointTestBase {

    public void testCreateWithoutInterfacesFail() {
        try {
            new HttpAvroClientEndpoint(m_endpointURL, m_configuration);
            fail("IllegalArgumentException expected!");
        }
        catch (IllegalArgumentException e) {
            // Ok; expected...
        }
    }

    /**
     * Tests that we can call the correct methods for a union type
     */
    public void testUnionTypeOk() throws Exception {
        Class<AvroUnionType> avroUnionType = AvroUnionType.class;
        Protocol p = ReflectData.get().getProtocol(avroUnionType);
        Message m = p.getMessages().get("m");

        HttpAvroClientEndpoint endpoint = new HttpAvroClientEndpoint(m_endpointURL, m_configuration, avroUnionType);

        AvroUnionType proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, AvroTestUtils.createResponse(m, null)));
        // calls "void m(int x, Void y)"
        proxy.m(Integer.valueOf(3), null);

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, AvroTestUtils.createResponse(m, 1)));
        // calls "int m(Void x, String y)"
        int result1 = (int) proxy.m(null, "foo");
        assertEquals(1, result1);

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, AvroTestUtils.createResponse(m, 3.1)));
        // calls "double m(int x, String y)"
        Number result2 = proxy.m(Integer.valueOf(3), "foo");
        assertEquals(3.1, result2.doubleValue(), 0.0001);
    }

    /**
     * Tests that if a remote service throws a checked exception, that this is
     * correctly unmarshalled by the client.
     */
    public void testHandleCheckedExceptionOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;
        Message m = AvroTestUtils.getMessage(ServiceA.class, "doException");

        HttpAvroClientEndpoint endpoint = new HttpAvroClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        Exception ex = new IOException("Invalid value!");
        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, AvroTestUtils.createResponse(m, ex)));

        try {
            // This should fail with an IOException...
            proxy.doException();
            fail("IOException expected!");
        }
        catch (IOException e) {
            // Ok; expected...
            assertEquals("Invalid value!", e.getMessage());
        }
    }

    /**
     * Tests that if a remote service returns a valid value, that this is
     * correctly unmarshalled by the client.
     */
    public void testHandleRemoteCallInvalidServiceLocationOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpAvroClientEndpoint endpoint = new HttpAvroClientEndpoint(new URL("http://does-not-exist"), m_configuration,
            serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        try {
            proxy.returnNull();
            fail("ServiceException expected!");
        }
        catch (ServiceException e) {
            // Ok; expected...
        }
    }

    /**
     * Tests that if a remote service returns a valid value, that this is
     * correctly unmarshalled by the client.
     */
    public void testHandleRemoteCallUnknownMethodOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpAvroClientEndpoint endpoint = new HttpAvroClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "2"));

        // Only positive numbers are allowed...
        assertEquals(m_endpointURL.hashCode(), proxy.hashCode());
    }

    /**
     * Tests that if a remote service returns a valid value, that this is
     * correctly unmarshalled by the client.
     */
    public void testHandleRemoteCallWithNullParameterOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;
        Protocol p = ReflectData.get().getProtocol(ServiceA.class);
        Message m = p.getMessages().get("tripeIt");
        HttpAvroClientEndpoint endpoint = new HttpAvroClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();
        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, AvroTestUtils.createResponse(m, 0)));

        // Only positive numbers are allowed...
        assertEquals(0, proxy.tripeIt(null));
    }

    /**
     * Tests that if a remote service returns a valid value, that this is
     * correctly unmarshalled by the client.
     */
    public void testHandleRemoteCallWithoutReturnValueOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;
        Message m = AvroTestUtils.getMessage(ServiceA.class, "doNothing");

        HttpAvroClientEndpoint endpoint = new HttpAvroClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, AvroTestUtils.createResponse(m, null)));

        // Only positive numbers are allowed...
        proxy.doNothing();
    }

    /**
     * Tests that if a remote service returns a valid value, that this is
     * correctly unmarshalled by the client.
     */
    public void testHandleRemoteCallWithReturnValueOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;
        Message m = AvroTestUtils.getMessage(ServiceA.class, "doubleIt");
        HttpAvroClientEndpoint endpoint = new HttpAvroClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, AvroTestUtils.createResponse(m, 2)));

        // Only positive numbers are allowed...
        assertEquals(2, proxy.doubleIt(1));
    }

    /**
     * Tests that if a remote service throws a runtime exception, that this is
     * correctly unmarshalled by the client.
     */
    public void testHandleRuntimeExceptionOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;
        Protocol p = ReflectData.get().getProtocol(ServiceA.class);
        Message m = p.getMessages().get("doubleIt");

        HttpAvroClientEndpoint endpoint = new HttpAvroClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();
        Exception ex = new IllegalArgumentException("Invalid value!");
        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, AvroTestUtils.createResponse(m, ex)));

        try {
            // This should fail with an IllegalArgumentException...
            proxy.doubleIt(0);
            fail("IllegalArgumentException expected!");
        }
        catch (IllegalArgumentException e) {
            // Ok; expected...
            assertEquals("Invalid value!", e.getMessage());
        }
    }

    /**
     * Tests that if a remote service throws a checked exception, that this is
     * correctly unmarshalled by the client.
     */
    public void testHandleServerExceptionOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpAvroClientEndpoint endpoint = new HttpAvroClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_INTERNAL_ERROR, ""));

        try {
            // This should fail with an IOException...
            proxy.doNothing();
            fail("ServiceException expected!");
        }
        catch (ServiceException e) {
            // Ok; expected...
        }
    }

    /**
     * Tests that the generic object methods, like {@link Object#hashCode()} &
     * {@link Object#equals(Object)} aren't forwarded to the server.
     */
    public void testObjectMethodsOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpAvroClientEndpoint endpoint = new HttpAvroClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        // Object#hashCode, Object#equals & Object#toString should be called on
        // the service location...
        assertEquals(m_endpointURL.hashCode(), proxy.hashCode());
        assertEquals(m_endpointURL.toString(), proxy.toString());
        assertFalse(proxy.equals(m_endpointURL));
        assertTrue(proxy.equals(proxy));
    }

    static class MutableURLStreamHandler extends URLStreamHandler {
        private final AtomicReference<URLConnection> m_urlConnRef = new AtomicReference<URLConnection>();

        public void setUpURLConnection(final TestURLConnection conn) {
            m_urlConnRef.set(conn);
        }

        @Override
        protected URLConnection openConnection(final URL u) throws IOException {
            return m_urlConnRef.get();
        }
    }
}
