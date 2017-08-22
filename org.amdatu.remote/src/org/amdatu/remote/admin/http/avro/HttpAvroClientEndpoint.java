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

import static java.net.HttpURLConnection.HTTP_OK;
import static org.amdatu.remote.IOUtil.closeSilently;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema.Field;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.osgi.framework.ServiceException;

/**
 * Implementation of an {@link InvocationHandler} that represents a remoted
 * service for one or more service interfaces.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpAvroClientEndpoint implements InvocationHandler {

    private static final int FATAL_ERROR_COUNT = 5;

    private final URL m_serviceURL;
    private final Object m_proxy;
    private final HttpAdminConfiguration m_configuration;
    private final Map<Method, Message> m_avroMessages;

    private ClientEndpointProblemListener m_problemListener;
    private int m_remoteErrors;

    public HttpAvroClientEndpoint(final URL serviceURL, final HttpAdminConfiguration configuration,
        final Class<?>... interfaceClasses) {
        if (interfaceClasses.length == 0) {
            throw new IllegalArgumentException("Need at least one interface to expose!");
        }
        // m_interfaceMethods = new HashMap<Method, String>();
        m_avroMessages = new HashMap<Method, Message>();
        m_serviceURL = serviceURL;
        m_proxy = Proxy.newProxyInstance(getClass().getClassLoader(), interfaceClasses, this);
        m_configuration = configuration;
        m_remoteErrors = 0;

        Protocol avpr; // map avro message schemas to java methods
        for (Class<?> interfaceClass : interfaceClasses) {
            avpr = ReflectData.get().getProtocol(interfaceClass);
            for (Method method : interfaceClass.getMethods()) {
                // m_interfaceMethods.put(method, getMethodSignature(method));
                m_avroMessages.put(method, avpr.getMessages().get(method.getName()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public final <T> T getServiceProxy() {
        return (T) m_proxy;
    }

    @Override
    public final Object invoke(final Object serviceProxy, final Method method, final Object[] args) throws Throwable {
        String methodName = method.getName();
        if ("equals".equals(methodName)) {
            // AMDATURS-119: Compare by identity, should be sufficient for the
            // general contract without the massive overhead of doing remote
            // calls...
            return serviceProxy == args[0];
        }
        else if (m_avroMessages.containsKey(method)) {
            return invokeRemoteMethod(method, args);
        }
        // Last resort: use the service URL for locks/monitors and string
        // representation...
        return method.invoke(m_serviceURL, args);
    }

    /**
     * @param problemListener
     *        the problem listener to set, can be <code>null</code>.
     */
    public void setProblemListener(final ClientEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    /**
     * Handles I/O exceptions by counting the number of times they occurred, and
     * if a certain threshold is exceeded closes the import registration for
     * this endpoint.
     *
     * @param e
     *        the exception to handle.
     */
    private void handleRemoteException(final IOException e) {
        if (m_problemListener != null) {
            if (++m_remoteErrors > FATAL_ERROR_COUNT) {
                m_problemListener.handleEndpointError(e);
            }
            else {
                m_problemListener.handleEndpointWarning(e);
            }
        }
    }

    /**
     * Does the invocation of the remote method adhering to any security
     * managers that might be installed.
     *
     * @param method
     *        the actual method to invoke;
     * @param arguments
     *        the arguments of the method to invoke;
     * @return the result of the method invocation, can be <code>null</code>.
     * @throws Exception
     *         in case the invocation failed in some way.
     */
    private Object invokeRemoteMethod(final Method method, final Object[] arguments) throws Throwable {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            try {
                return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        try {
                            return invokeRemoteMethodSecure(method, arguments);
                        }
                        catch (Throwable e) {
                            throw new ServiceException("TRANSPORT WRAPPER", e);
                        }
                    }
                });
            }
            catch (ServiceException e) {
                // All exceptions are wrapped in this exception, so we need to
                // rethrow its cause to get the actual exception back...
                throw e.getCause();
            }
        }
        else {
            return invokeRemoteMethodSecure(method, arguments);
        }
    }

    /**
     * Does the actual invocation of the remote method.
     * <p>
     * This method assumes that all security checks (if needed) are processed!
     * </p>
     *
     * @param method
     *        the actual method to invoke;
     * @param arguments
     *        the arguments of the method to invoke;
     * @return the result of the method invocation, can be <code>null</code>.
     * @throws Exception
     *         in case the invocation failed in some way.
     */
    private Object invokeRemoteMethodSecure(final Method method, final Object[] arguments) throws Throwable {

        HttpURLConnection connection = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        Object result = null;
        Throwable exception = null;
        try {
            connection = (HttpURLConnection) m_serviceURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setConnectTimeout(m_configuration.getConnectTimeout());
            connection.setReadTimeout(m_configuration.getReadTimeout());
            connection.setRequestProperty("Content-Type", "avro/binary");
            connection.connect();
            outputStream = connection.getOutputStream();

            request(method, arguments, outputStream);

            int rc = connection.getResponseCode();
            switch (rc) {
                case HTTP_OK:
                    inputStream = connection.getInputStream();
                    Object resultOrException = response(method, inputStream);
                    if (resultOrException instanceof Exception) {
                        exception = (Exception) resultOrException;
                    }
                    else {
                        result = resultOrException;
                    }
                    break;
                default:
                    throw new IOException("Unexpected HTTP response: " + rc + " " + connection.getResponseMessage());
            }
            // Reset this error counter upon each successful request...
            m_remoteErrors = 0;
        }
        catch (IOException e) {
            handleRemoteException(e);
            throw new ServiceException("Remote service invocation failed: " + e.getMessage(), ServiceException.REMOTE,
                e);
        }
        finally {
            closeSilently(inputStream, outputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (exception != null) {
            throw exception;
        }
        return result;
    }

    /**
     * Returns the response/result of {@code method} from {@code inputStream}.
     * Note that the result can be an {@code Exception}.
     *
     * @param method
     *        the requested method
     * @param inputStream
     *        the stream to read from
     * @return the result of requested RPC. Note that the result can be an
     *         {@code Exception}.
     * @throws IOException
     *         if an I/O error occurs
     */
    public Object response(final Method method, final InputStream inputStream) throws IOException {
        Message m = m_avroMessages.get(method);

        if (m == null) {
            throw new IllegalArgumentException("No avro message type found for " + method);
        }

        return response(m, inputStream);
    }

    public static Object response(final Message m, final InputStream inputStream) throws IOException {
        Decoder in = DecoderFactory.get().binaryDecoder(inputStream, null);

        if (in.readBoolean()) { // error
            return new ReflectDatumReader<Exception>(m.getErrors()).read(null, in);
        }
        else { // return value
            return new ReflectDatumReader<Object>(m.getResponse()).read(null, in);
        }
    }

    /**
     * Writes a RPC request for {@code method} with {@code args} to
     * {@code outputStream}. Note that this operation closes
     * {@code outputStream}.
     *
     * @param method
     *        the requested method
     * @param args
     *        the requested method arguments
     * @param outputStream
     *        the stream to which the request is written to
     * @throws IOException
     *         if an I/O error occurs
     */
    public void request(final Method method, final Object[] args, final OutputStream outputStream) throws IOException {
        Message m = m_avroMessages.get(method);

        if (m == null) {
            throw new IllegalArgumentException("No avro message type found for " + method);
        }

        request(m, args, outputStream);
    }

    /**
     * Writes a RPC request message in Avro binary format to
     * {@code outputStream}. The request message consists of the method name
     * followed by the arguments in order. Note that this operation closes
     * {@code outputStream}.
     *
     * @param m
     *        the requested Avro message
     * @param args
     *        the requested message arguments
     * @param outputStream
     *        the stream to which the request is written to
     * @throws IOException
     *         if an I/O error occurs
     */
    public static void request(final Message m, final Object[] args, final OutputStream outputStream)
        throws IOException {
        BinaryEncoder out = EncoderFactory.get().binaryEncoder(outputStream, null);

        out.writeString(m.getName()); // write method name

        int i = 0;
        for (Field field : m.getRequest().getFields()) { // write fields
            new ReflectDatumWriter<Object>(field.schema()).write(args[i++], out);
        }

        out.flush();
        outputStream.close();
    }
}
