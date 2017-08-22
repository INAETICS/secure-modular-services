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
package org.amdatu.remote.itest.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * Self-registering utility that uses an {@link RemoteServiceAdminListener} to await an event of specified
 * type and matching an optional filter.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class BlockingRemoteServiceAdminListener {

    private final BundleContext m_context;
    private final int m_type;
    private final Filter m_filter;

    private CountDownLatch m_latch = new CountDownLatch(1);
    private ServiceRegistration<?> m_registration;

    public BlockingRemoteServiceAdminListener(BundleContext context, int type, String filter) throws Exception {
        m_context = context;
        m_type = type;
        if (filter != null) {
            m_filter = FrameworkUtil.createFilter(filter);
        }
        else {
            m_filter = null;
        }
    }

    public synchronized void register() {
        unregister();
        m_registration =
            m_context.registerService(RemoteServiceAdminListener.class, new InterbalRemoteServcieAdminListener(), null);
    }

    public synchronized void unregister() {
        if (m_registration != null) {
            m_registration.unregister();
            m_registration = null;
        }
    }

    public synchronized void reset() {
        m_latch = new CountDownLatch(1);
    }

    public final boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        checkRegistrationState();
        try {
            return m_latch.await(timeout, unit);
        }
        catch (InterruptedException e) {
            return false;
        }
    }

    private class InterbalRemoteServcieAdminListener implements RemoteServiceAdminListener {

        @Override
        public void remoteAdminEvent(RemoteServiceAdminEvent event) {

            if (event.getType() != m_type) {
                return;
            }
            if (m_filter == null) {
                m_latch.countDown();
                return;
            }

            if (event.getExportReference() != null) {
                EndpointDescription endpoint = event.getExportReference().getExportedEndpoint();
                if (endpoint != null && m_filter.matches(endpoint.getProperties())) {
                    m_latch.countDown();
                    return;
                }
            }

            if (event.getImportReference() != null) {
                EndpointDescription endpoint = event.getImportReference().getImportedEndpoint();
                if (endpoint != null && m_filter.matches(endpoint.getProperties())) {
                    m_latch.countDown();
                    return;
                }
            }
        }
    }

    private void checkRegistrationState() {
        if (m_registration == null) {
            throw new IllegalStateException("Unregistered state! Use reset if you want to reuse this listener.");
        }
    }

}
