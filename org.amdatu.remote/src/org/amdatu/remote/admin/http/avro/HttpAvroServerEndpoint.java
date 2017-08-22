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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.amdatu.remote.IOUtil;
import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Servlet that represents a remoted local service.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpAvroServerEndpoint {

    private static final int FATAL_ERROR_COUNT = 5;

    private static final String AVRO_BINARY = "avro/binary";

    protected final String CONTENT_TYPE = AVRO_BINARY;

    private final BundleContext m_bundleContext;
    private final ServiceReference<?> m_serviceReference;
    private final Map<String, Method> m_interfaceMethods;
    private final Map<String, Message> m_avroMessages;

    private ServerEndpointProblemListener m_problemListener;
    private String m_methodName;
    private int m_localErrors;
    private int m_httpResponseError;

    public HttpAvroServerEndpoint(final BundleContext context, final ServiceReference<?> reference,
        final Class<?>... interfaceClasses) {

        m_bundleContext = context;
        m_serviceReference = reference;
        m_interfaceMethods = new HashMap<String, Method>();
        m_avroMessages = new HashMap<String, Message>();

        Protocol p;
        Message message;
        for (Class<?> interfaceClass : interfaceClasses) {
            p = ReflectData.get().getProtocol(interfaceClass);
            for (Method method : interfaceClass.getMethods()) {
                message = p.getMessages().get(method.getName());
                if (message != null) {
                    // Although we're accessing a public (interface) method, the
                    // *service* implementation
                    // itself can be non-public. This check appears to be fixed
                    // in
                    // recent Java versions...
                    method.setAccessible(true);
                    m_interfaceMethods.put(message.getName(), method);
                    m_avroMessages.put(message.getName(), message);
                }
            }
        }
    }

    /**
     * @param problemListener
     *        the problem listener to set, can be <code>null</code>.
     */
    public void setProblemListener(final ServerEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    public void invokeService(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {
        InputStream in = req.getInputStream();
        Object service = null;
        try {
            service = m_bundleContext.getService(m_serviceReference);
            if (service == null) {
                handleLocalException(null);
                resp.sendError(SC_SERVICE_UNAVAILABLE);
                return;
            }

            // decodes the request and invokes the right method.
            m_httpResponseError = 0;
            Object resultOrException = request(in, resp, service);
            if (m_httpResponseError != 0) {
                resp.sendError(m_httpResponseError);
                return;
            }

            resp.setStatus(SC_OK);
            resp.setContentType(CONTENT_TYPE);

            OutputStream outputStream = resp.getOutputStream();
            response(resultOrException, outputStream);

            // All is fine.. reset the local error count
            m_localErrors = 0;
        }
        finally {
            IOUtil.closeSilently(in);
            if (service != null) {
                service = null;
                try {
                    m_bundleContext.ungetService(m_serviceReference);
                }
                catch (Exception e) {
                    // ignore... we at least tried
                }
            }
        }
    }

    /**
     * Write {@code resultOrException} as response to {@code outputStream}.
     *
     * @param resultOrException
     *        the result to send back
     * @param outputStream
     *        the stream used to write to
     * @throws IOException
     */
    protected void response(final Object resultOrException, final OutputStream outputStream) throws IOException {
        Message m = m_avroMessages.get(m_methodName);
        response(m, resultOrException, outputStream);
    }

    /**
     * Write {@code resultOrException} as Avro binary format to
     * {@code outputStream} using the schema of {@code m}.
     *
     * @param m
     *        the message schema used for decoding the result
     * @param resultOrException
     *        the result to decode
     * @param outputStream
     *        the stream sued to write to
     * @throws IOException
     */
    public static void response(final Message m, final Object resultOrException, final OutputStream outputStream)
        throws IOException {
        Encoder out = EncoderFactory.get().binaryEncoder(outputStream, null);

        if (resultOrException instanceof Exception) {
            out.writeBoolean(true);
            Throwable exception = unwrapException((Exception) resultOrException);
            new ReflectDatumWriter<Object>(m.getErrors()).write(exception, out);
        }
        else {
            out.writeBoolean(false);
            new ReflectDatumWriter<Object>(m.getResponse()).write(resultOrException, out);
        }
        out.flush();
        outputStream.close();
    }

    private Object request(final InputStream inputStream, final HttpServletResponse resp, final Object service)
        throws IOException {
        BinaryDecoder in = DecoderFactory.get().binaryDecoder(inputStream, null);
        // the first element of the request should be the method name
        m_methodName = in.readString(null).toString();

        if (m_methodName == null) {
            m_httpResponseError = SC_NOT_FOUND;
            return null;
        }

        Method method = m_interfaceMethods.get(m_methodName);
        Message m = m_avroMessages.get(m_methodName);

        if (method == null || m == null) {
            m_httpResponseError = SC_NOT_FOUND;
            return null;
        }

        Object[] args;
        try {
            args = decodeArguments(m.getRequest(), m.getRequest(), in);
        }
        catch (Exception e) {
            m_httpResponseError = SC_BAD_REQUEST;
            return null;
        }

        try {
            return method.invoke(service, args);
        }
        catch (Exception e) {
            return e;
        }
    }

    /**
     * Writes all method signatures as a flat JSON array to the given
     * HttpServletResponse
     *
     * @param req
     *        the HttpServletRequest
     * @param resp
     *        the HttpServletResponse
     * @throws IOException
     */
    public void listMethodSignatures(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.setStatus(SC_OK);
        resp.setContentType(CONTENT_TYPE);

        OutputStream outputStream = resp.getOutputStream();
        Encoder out = EncoderFactory.get().binaryEncoder(outputStream, null);
        Schema schema = Schema.createArray(Schema.create(Type.STRING));
        new ReflectDatumWriter<>(schema).write(m_interfaceMethods.keySet(), out);
        out.flush();
        outputStream.close();
    }

    /**
     * Decode the requested arguments from {@code in} using the resolved schema
     * of {@code remote} and {@code local}.
     *
     * @param remote
     *        the schema of the client/sender
     * @param local
     *        the schema of the server
     * @param in
     *        stream to read from
     * @return the decoded arguments
     * @throws IOException
     */
    private Object[] decodeArguments(final Schema remote, final Schema local, final BinaryDecoder in)
        throws IOException {
        GenericRecord gr = new ReflectDatumReader<GenericRecord>(remote, local).read(null, in);
        int numberOfParameters = local.getFields().size();
        Object[] args = new Object[numberOfParameters];
        int i = 0;
        for (Schema.Field param : local.getFields()) {
            args[i++] = gr.get(param.name());
        }
        return args;
    }

    /**
     * Handles I/O exceptions by counting the number of times they occurred, and
     * if a certain threshold is exceeded closes the import registration for
     * this endpoint.
     *
     * @param e
     *        the exception to handle.
     */
    private void handleLocalException(final IOException e) {
        if (m_problemListener != null) {
            if (++m_localErrors > FATAL_ERROR_COUNT) {
                m_problemListener.handleEndpointError(e);
            }
            else {
                m_problemListener.handleEndpointWarning(e);
            }
        }
    }

    /**
     * Unwraps a given {@link Exception} into a more concrete exception if it
     * represents an {@link InvocationTargetException}.
     *
     * @param e
     *        the exception to unwrap, should not be <code>null</code>.
     * @return the (unwrapped) throwable or exception, never <code>null</code>.
     */
    private static Throwable unwrapException(final Exception e) {
        if (e instanceof InvocationTargetException) {
            return ((InvocationTargetException) e).getTargetException();
        }
        return e;
    }
}
