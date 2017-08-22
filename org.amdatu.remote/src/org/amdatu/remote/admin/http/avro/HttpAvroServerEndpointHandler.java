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

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.amdatu.remote.AbstractComponentDelegate;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * RSA component that handles all server endpoints.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpAvroServerEndpointHandler extends AbstractComponentDelegate {

    private final Map<String, HttpAvroServerEndpoint> m_handlers = new HashMap<String, HttpAvroServerEndpoint>();
    private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

    private final RemoteServiceAdminFactory m_manager;

    private static final String CONTENT_TYPE = "avro/binary";

    public HttpAvroServerEndpointHandler(final RemoteServiceAdminFactory manager) {
        super(manager);
        m_manager = manager;
    }

    @Override
    protected void startComponentDelegate() {
        try {
            m_manager.getHttpService().registerServlet(getServletAlias(), new ServerEndpointServlet(), null, null);
        }
        catch (Exception e) {
            logError("Failed to initialize due to configuration problem!", e);
            throw new IllegalStateException("Configuration problem", e);
        }
    }

    @Override
    protected void stopComponentDelegate() {
        m_manager.getHttpService().unregister(getServletAlias());
    }

    /**
     * Returns the runtime URL for a specified Endpoint ID.
     *
     * @param endpointId The Endpoint ID
     * @return The URL
     * @throws IllegalArgumentException If the Endpoint ID is not a valid URL path segment
     */
    public URL getEndpointURL(final String endpointId) {
        try {
            return new URL(m_manager.getBaseURL(), endpointId);
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid endpoint id", e);
        }
    }

    /**
     * Add a Server Endpoint.
     *
     * @param reference The local Service Reference
     * @param endpoint The Endpoint Description
     */
    public HttpAvroServerEndpoint addEndpoint(final ServiceReference<?> reference, final EndpointDescription endpoint,
        final Class<?>[] interfaces) {

        HttpAvroServerEndpoint serverEndpoint = new HttpAvroServerEndpoint(getBundleContext(), reference, interfaces);
        m_lock.writeLock().lock();
        try {
            m_handlers.put(endpoint.getId(), serverEndpoint);
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serverEndpoint;
    }

    /**
     * Remove a Server Endpoint.
     *
     * @param endpoint The Endpoint Description
     */
    public HttpAvroServerEndpoint removeEndpoint(final EndpointDescription endpoint) {
        HttpAvroServerEndpoint serv;

        m_lock.writeLock().lock();
        try {
            serv = m_handlers.remove(endpoint.getId());
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serv;
    }

    private HttpAvroServerEndpoint getHandler(final String id) {
        m_lock.readLock().lock();
        try {
            return m_handlers.get(id);
        }
        finally {
            m_lock.readLock().unlock();
        }
    }

    private String getServletAlias() {
        String alias = m_manager.getBaseURL().getPath();
        if (!alias.startsWith("/")) {
            alias = "/" + alias;
        }
        if (alias.endsWith("/")) {
            alias = alias.substring(0, alias.length() - 1);
        }
        return alias;
    }

    /**
     * Writes all endpoint ids as a flat JSON array to the given HttpServletResponse
     *
     * @param req the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws IOException
     */
    public void listEndpointIds(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.setStatus(SC_OK);
        resp.setContentType(CONTENT_TYPE);

        Schema schema = Schema.createArray(Schema.create(Type.STRING));
        OutputStream outputStream = resp.getOutputStream();
        Encoder out = EncoderFactory.get().binaryEncoder(outputStream, null);
        // TODO from Damiaan van der Kruk: check if whe should lock here?
        new ReflectDatumWriter<>(schema).write(m_handlers.keySet(), out);
        out.flush();
        outputStream.close();
    }

    /**
     * Internal Servlet that handles all calls.
     */
    private class ServerEndpointServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        private final Pattern PATH_PATTERN = Pattern.compile("^\\/{0,1}([A-Za-z0-9-_]+)\\/{0,1}$");

        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            Matcher matcher = PATH_PATTERN.matcher(pathInfo);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + pathInfo);
                return;
            }
            String endpointId = matcher.group(1);

            HttpAvroServerEndpoint handler = getHandler(endpointId);
            if (handler != null) {
                try {
                    handler.invokeService(req, resp);
                }
                catch (Exception e) {
                    logError("Server Endpoint Handler failed: %s", e, endpointId);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
            else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

            // provide endpoint information via http get

            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            // request on root will return an array of endpoint ids
            if (pathInfo.equals("") || pathInfo.equals("/")) {
                listEndpointIds(req, resp);
                return;
            }

            // handle requested endpoint
            Matcher matcher = PATH_PATTERN.matcher(pathInfo);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + pathInfo);
                return;
            }

            String endpointId = matcher.group(1);

            HttpAvroServerEndpoint handler = getHandler(endpointId);
            if (handler != null) {
                try {
                    handler.listMethodSignatures(req, resp);
                }
                catch (Exception e) {
                    logError("Server Endpoint Handler failed: %s", e, endpointId);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
            else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }
}
