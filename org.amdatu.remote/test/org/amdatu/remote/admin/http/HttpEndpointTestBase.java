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
package org.amdatu.remote.admin.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public abstract class HttpEndpointTestBase extends TestCase implements URLStreamHandlerFactory {

	private static AtomicBoolean URL_FACTORY_INSTALLED = new AtomicBoolean(false);
	private static MutableURLStreamHandler m_urlHandler = new MutableURLStreamHandler();

	protected URL m_endpointURL;

	@Override
	protected void setUp() throws Exception {
		if (URL_FACTORY_INSTALLED.compareAndSet(false, true)) {
			URL.setURLStreamHandlerFactory(this);
		}
		m_endpointURL = new URL("test://");
	}

	protected void setUpURLStreamHandler(final TestURLConnection conn) {
		m_urlHandler.setUpURLConnection(conn);
	}

	static class MutableURLStreamHandler extends URLStreamHandler {
		private final AtomicReference<URLConnection> m_urlConnRef = new AtomicReference<URLConnection>();

		public void setUpURLConnection(TestURLConnection conn) {
			m_urlConnRef.set(conn);
		}

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return m_urlConnRef.get();
		}
	}

	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if ("test".equals(protocol)) {
			return m_urlHandler;
		}
		return null;
	}

	protected static class TestURLConnection extends HttpURLConnection {
		private final ByteArrayOutputStream m_baos;
		private final ByteArrayInputStream m_bais;

		public TestURLConnection(int rc, byte[] result) {
			super(null);

			m_baos = new ByteArrayOutputStream();
			m_bais = new ByteArrayInputStream(result);

			responseCode = rc;
		}
		
		public TestURLConnection(int rc, String result) {
			this(rc, result.getBytes());
		}

		@Override
		public void connect() throws IOException {
			// Nop
		}

		@Override
		public void disconnect() {
			// Nop
		}

		@Override
		public InputStream getErrorStream() {
			if (responseCode < 400) {
				return super.getErrorStream();
			}
			return m_bais;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if (responseCode >= 400) {
				return super.getInputStream();
			}
			return m_bais;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return m_baos;
		}

		@Override
		public int getResponseCode() throws IOException {
			return responseCode;
		}

		@Override
		public boolean usingProxy() {
			return false;
		}
	}
}
