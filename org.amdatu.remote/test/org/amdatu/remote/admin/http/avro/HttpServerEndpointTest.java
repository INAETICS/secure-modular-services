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

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.amdatu.remote.admin.http.TestUtil.ServiceA;
import org.amdatu.remote.admin.http.TestUtil.ServiceAImpl;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import junit.framework.TestCase;

/**
 * Test cases for {@link HttpAvroServerEndpoint}.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class HttpServerEndpointTest extends TestCase {

    private BundleContext m_context;
    private ServiceReference<Object> m_serviceRef;
    private HttpServletRequest m_servletRequest;
    private HttpServletResponse m_servletResponse;
    private MockServletOutputStream m_outputStream;

    /**
     * Tests that we can call a method that fails with a checked exception.
     */
    public void testCallMethodWithCheckedExceptionOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());
        Message m = AvroTestUtils.getMessage(ServiceA.class, "doException");

        mockServiceLookup(service, createRequest(m, new Object[] {}));

        HttpAvroServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).setStatus(SC_OK);
        verify(service).doException();

        Object o = getResponse(m);

        assertTrue(o instanceof Throwable);
        assertEquals(IOException.class, o.getClass());
        assertEquals("Exception!", ((Throwable) o).getMessage());
    }

    /**
     * Tests that we can call a void-method.
     */
    public void testCallMethodWithNullResultOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());
        Message m = AvroTestUtils.getMessage(ServiceA.class, "returnNull");
        mockServiceLookup(service, createRequest(m, null));

        HttpAvroServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).setStatus(SC_OK);
        verify(service).returnNull();

        Object o = getResponse(m);
        assertEquals(null, o);
    }

    /**
     * Tests that we can call a void-method.
     */
    public void testCallMethodWithoutServiceOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, createRequest("qqq"));

        HttpAvroServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).sendError(SC_NOT_FOUND);
        verifyNoMoreInteractions(service);

        m_outputStream.assertContent("");
    }

    /**
     * Tests that we can call a method successfully and process its result.
     */
    public void testCallMethodWithResultOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());
        Message m = AvroTestUtils.getMessage(ServiceA.class, "doubleIt");

        mockServiceLookup(service, createRequest(m, new Object[] { 3 }));

        HttpAvroServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).setStatus(SC_OK);
        verify(service).doubleIt(eq(3));

        assertEquals(6, getResponse(m));
    }

    /**
     * Tests that we can call a method that fails with a runtime exception.
     */
    public void testCallMethodWithRuntimeExceptionOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());
        Message m = AvroTestUtils.getMessage(ServiceA.class, "doubleIt");

        mockServiceLookup(service, createRequest(m, new Object[] { 0 }));

        HttpAvroServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).setStatus(SC_OK);
        verify(service).doubleIt(eq(0));

        Object o = getResponse(m);
        assertTrue(o instanceof Throwable);
        assertEquals(IllegalArgumentException.class, o.getClass());
        assertEquals("Invalid value!", ((Throwable) o).getMessage());
    }

    /**
     * Tests that we can call a void-method.
     */
    public void testCallUnknownMethodOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, createRequest("hashCode"));

        HttpAvroServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).sendError(SC_NOT_FOUND);
        verifyNoMoreInteractions(service);

        m_outputStream.assertContent("");
    }

    /**
     * Tests that we can call a void-method.
     */
    public void testCallVoidMethodOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());
        Message m = AvroTestUtils.getMessage(ServiceA.class, "doNothing");

        mockServiceLookup(service, createRequest(m, new Object[] {}));

        HttpAvroServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).setStatus(SC_OK);
        verify(service).doNothing();
    }

    /**
     * Tests that we can call a void-method.
     */
    public void testCallWithoutMethodOk() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        ServiceAImpl service = spy(new ServiceAImpl());

        mockServiceLookup(service, createRequest(""));

        HttpAvroServerEndpoint endpoint = createEndpoint(type);
        endpoint.invokeService(m_servletRequest, m_servletResponse);

        verify(m_servletResponse).sendError(SC_NOT_FOUND);
        verifyNoMoreInteractions(service);

        m_outputStream.assertContent("");
    }

    /**
     * Test that listing method signatures is correct
     */
    public void testListMethodSignatures() throws Exception {
        Class<ServiceA> type = ServiceA.class;
        HttpAvroServerEndpoint endpoint = createEndpoint(type);
        endpoint.listMethodSignatures(m_servletRequest, m_servletResponse);
        Decoder in = DecoderFactory.get().binaryDecoder(m_outputStream.getRawContent(), null);
        Schema schema = Schema.createArray(Schema.create(Type.STRING));
        schema.addProp("java-class", "java.util.ArrayList");
        ArrayList<String> methodNames = new ReflectDatumReader<ArrayList<String>>(schema).read(null, in);

        Method[] methods = type.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            assertTrue(methodNames.contains(method.getName()));
        }
    }

    @Override
    protected void setUp() throws Exception {
        m_outputStream = new MockServletOutputStream();

        m_context = mock(BundleContext.class);
        m_serviceRef = mock(ServiceReference.class);
        m_servletRequest = mock(HttpServletRequest.class);
        m_servletResponse = mock(HttpServletResponse.class);
        when(m_servletResponse.getOutputStream()).thenReturn(m_outputStream);
    }

    private Object getResponse(final Message m) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(m_outputStream.getRawContent());
        Object o = HttpAvroClientEndpoint.response(m, in);
        return o;
    }

    private byte[] createRequest(final String methodName) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder out = EncoderFactory.get().binaryEncoder(outputStream, null);
        out.writeString(methodName);
        out.flush();
        outputStream.close();
        return outputStream.toByteArray();
    }

    private byte[] createRequest(final Message m, final Object[] args) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpAvroClientEndpoint.request(m, args, out);
        out.close();
        return out.toByteArray();
    }

    private HttpAvroServerEndpoint createEndpoint(final Class<?>... interfaces) {
        return new HttpAvroServerEndpoint(m_context, m_serviceRef, interfaces);
    }

    private void mockServiceLookup(final Object service, final byte[] content) throws IOException {
        when(m_servletRequest.getInputStream()).thenReturn(new MockServletInputStream(content));
        when(m_context.getService(eq(m_serviceRef))).thenReturn(service);
    }

    static class MockServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream m_bais;

        public MockServletInputStream(final byte[] input) {
            m_bais = new ByteArrayInputStream(input);
        }

        public MockServletInputStream(final String input) {
            this(input.getBytes());
        }

        @Override
        public int read() throws IOException {
            return m_bais.read();
        }
    }

    static class MockServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream m_baos = new ByteArrayOutputStream();

        @Override
        public void write(final int b) throws IOException {
            m_baos.write(b);
        }

        void assertContent(final byte[] content) {
            assertEquals(content, m_baos.toByteArray());
        }

        void assertContent(final String content) {
            assertEquals(content, getBodyContent());
        }

        byte[] getRawContent() {
            return m_baos.toByteArray();
        }

        String getBodyContent() {
            return new String(m_baos.toByteArray());
        }
    }
}
