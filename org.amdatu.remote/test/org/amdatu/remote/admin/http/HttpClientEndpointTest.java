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
package org.amdatu.remote.admin.http;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

import java.io.IOException;
import java.net.URL;

import org.amdatu.remote.admin.http.TestUtil.BoundType;
import org.amdatu.remote.admin.http.TestUtil.GenericType;
import org.amdatu.remote.admin.http.TestUtil.ServiceA;
import org.osgi.framework.ServiceException;

/**
 * Test cases for {@link HttpClientEndpoint}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class HttpClientEndpointTest extends HttpJsonEndpointTestBase {

    public void testCreateWithoutInterfacesFail() {
        try {
            new HttpClientEndpoint(m_endpointURL, m_configuration);
            fail("IllegalArgumentException expected!");
        }
        catch (IllegalArgumentException e) {
            // Ok; expected...
        }
    }

    /**
     * Tests that we can call the correct methods for a generic type, with unbound types.
     * 
     * <p>Note that although the generic types are bound, the methods from the *generic*
     * interface are called (due to type erasure).</p>
     */
    @SuppressWarnings("rawtypes")
    public void testGenericTypeOk() throws Exception {
        Class<GenericType> genericType = GenericType.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, genericType);

        GenericType<Integer, String> proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{}"));

        // calls "void m(X x)"
        proxy.m(Integer.valueOf(3));

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{ \"r\" : 1}"));

        // calls "int m(Y y)"
        int result1 = proxy.m("foo");

        assertEquals(1, result1);

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{ \"r\" : 3.1}"));

        // calls "X m(X x, Y y)"
        Number result2 = proxy.m(Integer.valueOf(3), "foo");

        assertEquals(3.1, result2.doubleValue(), 0.0001);
    }

    /**
     * Tests that if a remote service throws a checked exception, that this is correctly unmarshalled by the client.
     */
    public void testHandleCheckedExceptionOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK,
            "{ \"e\" : {\"type\":\"java.io.IOException\",\"msg\":\"Invalid value!\",\"stacktrace\":[]}}"));

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
     * Tests that if a remote service returns a valid value, that this is correctly unmarshalled by the client.
     */
    public void testHandleRemoteCallInvalidServiceLocationOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpClientEndpoint endpoint =
            new HttpClientEndpoint(new URL("http://does-not-exist"), m_configuration, serviceAType);

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
     * Tests that if a remote service returns a invalid value, that this is correctly signaled by the client.
     */
    public void testHandleRemoteCallReturnsInvalidTypeOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{ \"r\" : [2] }"));

        try {
            proxy.doubleIt(1);
            fail("ServiceException expected!");
        }
        catch (ServiceException e) {
            // Ok; expected...
        }
    }

    /**
     * Tests that if a remote service returns a valid value, that this is correctly unmarshalled by the client.
     */
    public void testHandleRemoteCallUnknownMethodOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "2"));

        // Only positive numbers are allowed...
        assertEquals(m_endpointURL.hashCode(), proxy.hashCode());
    }

    /**
     * Tests that if a remote service returns a valid value, that this is correctly unmarshalled by the client.
     */
    public void testHandleRemoteCallWithNullParameterOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{ \"r\" : 0}"));

        // Only positive numbers are allowed...
        assertEquals(0, proxy.tripeIt(null));
    }

    /**
     * Tests that if a remote service returns a valid value, that this is correctly unmarshalled by the client.
     */
    public void testHandleRemoteCallWithoutReturnValueOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{}"));

        // Only positive numbers are allowed...
        proxy.doNothing();
    }

    /**
     * Tests that if a remote service returns a valid value, that this is correctly unmarshalled by the client.
     */
    public void testHandleRemoteCallWithReturnValueOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{ \"r\" : 2}"));

        // Only positive numbers are allowed...
        assertEquals(2, proxy.doubleIt(1));
    }

    /**
     * Tests that if a remote service throws a runtime exception, that this is correctly unmarshalled by the client.
     */
    public void testHandleRuntimeExceptionOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK,
            "{ \"e\" :{\"type\":\"java.lang.IllegalArgumentException\",\"msg\":\"Invalid value!\",\"stacktrace\":[]}}"));

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
     * Tests that if a remote service throws a checked exception, that this is correctly unmarshalled by the client.
     */
    public void testHandleServerExceptionOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, serviceAType);

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
     * Tests that the generic object methods, like {@link Object#hashCode()} & {@link Object#equals(Object)} aren't forwarded to the server.
     */
    public void testObjectMethodsOk() throws Exception {
        Class<ServiceA> serviceAType = ServiceA.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, serviceAType);

        ServiceA proxy = endpoint.getServiceProxy();

        // Object#hashCode, Object#equals & Object#toString should be called on the service location...
        assertEquals(m_endpointURL.hashCode(), proxy.hashCode());
        assertEquals(m_endpointURL.toString(), proxy.toString());
        assertFalse(proxy.equals(m_endpointURL));
        assertTrue(proxy.equals(proxy));
    }

    /**
     * Tests that for a concrete instance of a generic type, the correct methods are called.
     * 
     * <p>Note that although the generic types are bound, the methods from the *generic*
     * interface are called (due to type erasure).</p>
     */
    public void testTypeErasureOk() throws Exception {
        Class<BoundType> type = BoundType.class;

        HttpClientEndpoint endpoint = new HttpClientEndpoint(m_endpointURL, m_configuration, type);

        BoundType proxy = endpoint.getServiceProxy();

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{}"));

        // calls "void m(Long x)"
        proxy.m(Long.valueOf(3));

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{ \"r\" : 1}"));

        // calls "int m(String y)"
        int result1 = proxy.m("foo");

        assertEquals(1, result1);

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{ \"r\" : 100000000000000}"));

        // calls "Long m(Long x, String y)"
        Number result2 = proxy.m(Long.valueOf(3), "foo");

        assertEquals(100000000000000L, result2.longValue());

        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, "{ \"r\" : \"foo\"}"));

        // calls "String m(String y, Long x)"
        String result3 = proxy.m("foo", Long.valueOf(3));

        assertEquals("foo", result3);
    }
}
