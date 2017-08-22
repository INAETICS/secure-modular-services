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
package org.amdatu.remote.itest.junit.common;

import java.util.concurrent.TimeUnit;

import org.amdatu.remote.AbstractEndpointPublishingComponent;
import org.amdatu.remote.itest.util.AbstractBlockingEndpointListener;
import org.amdatu.remote.itest.util.BlockingEndpointEventListener;
import org.amdatu.remote.itest.util.BlockingEndpointListener;
import org.amdatu.remote.itest.util.ITestUtil;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * Test for testing edge cases in Endpoint (Event) Listener handling by {@link AbstractEndpointPublishingComponent}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("deprecation")
public final class EndpointPublishingComponentTest extends AbstractCommonTest {

    private static class DummyEndpointPublishingComponent extends AbstractEndpointPublishingComponent {

        public DummyEndpointPublishingComponent() {
            super("dummy", "dummy");
        }

        public void callEndpointAdded(EndpointDescription endpoint) {
            endpointAdded(endpoint);
        }

        public void callEndpointRemoved(EndpointDescription endpoint) {
            endpointRemoved(endpoint);
        }

        public void callEndpointModified(EndpointDescription endpoint) {
            endpointModified(endpoint);
        }
    }

    private DummyEndpointPublishingComponent m_dummy = new DummyEndpointPublishingComponent();
    private Component m_component;

    @Override
    protected void configureServices() throws Exception {

        DependencyManager dependencyManager = getChildContext().getDependencyManager();

        m_component = dependencyManager.createComponent()
            .setImplementation(m_dummy)
            .add(dependencyManager.createServiceDependency()
                .setService(EndpointEventListener.class, "(!(discovery=true))")
                .setCallbacks("eventListenerAdded", "eventListenerModified", "eventListenerRemoved")
                .setRequired(false))
            .add(dependencyManager.createServiceDependency()
                .setService(EndpointListener.class, "(!(discovery=true))")
                .setCallbacks("listenerAdded", "listenerModified", "listenerRemoved")
                .setRequired(false))
            .add(dependencyManager.createServiceDependency()
                .setService(LogService.class)
                .setRequired(false));

        dependencyManager.add(m_component);
    }

    public void testEndpointPublishingComponent() throws Exception {
        doTestEndpointListener();
        doTestEndpointEventListener();
    }

    public void doTestEndpointListener() throws Exception {

        EndpointDescription endpoint = ITestUtil.createEndpointDescription("uuid1", "Interface1", 1l);
        BlockingEndpointListener listener =
            new BlockingEndpointListener(getChildContext().getBundleContext(), endpoint, "("
                + RemoteConstants.ENDPOINT_FRAMEWORK_UUID + "=uuid1)");

        try {
            listener.register();
            m_dummy.callEndpointAdded(endpoint);
            assertAddedCallback(listener);

            listener.unregister();

            listener.reset();
            listener.register();
            assertAddedCallback(listener);

            listener.reset();
            m_dummy.callEndpointModified(endpoint);
            assertRemovedAddedCallbacks(listener);

            listener.reset();
            listener.changeScopeFilter("(" + RemoteConstants.ENDPOINT_FRAMEWORK_UUID + "=uuid2)");
            assertRemovedCallback(listener);

            listener.reset();
            listener.changeScopeFilter("(" + RemoteConstants.ENDPOINT_FRAMEWORK_UUID + "=uuid1)");
            assertAddedCallback(listener);

            listener.reset();
            m_dummy.callEndpointRemoved(endpoint);
            assertRemovedCallback(listener);
        }
        finally {
            listener.unregister();
        }
    }

    public void doTestEndpointEventListener() throws Exception {

        EndpointDescription endpoint = ITestUtil.createEndpointDescription("uuid1", "Interface1", 1l);
        BlockingEndpointEventListener listener =
            new BlockingEndpointEventListener(getChildContext().getBundleContext(), endpoint, "("
                + RemoteConstants.ENDPOINT_FRAMEWORK_UUID + "=uuid1)");

        try {
            listener.register();
            m_dummy.callEndpointAdded(endpoint);
            assertAddedCallback(listener);

            listener.unregister();

            listener.reset();
            listener.register();
            assertAddedCallback(listener);

            listener.reset();
            m_dummy.callEndpointModified(endpoint);
            assertModifiedCallback(listener);

            listener.reset();
            listener.changeScopeFilter("(" + RemoteConstants.ENDPOINT_FRAMEWORK_UUID + "=uuid2)");
            assertEndmatched(listener);

            listener.reset();
            listener.changeScopeFilter("(" + RemoteConstants.ENDPOINT_FRAMEWORK_UUID + "=uuid1)");
            assertAddedCallback(listener);

            listener.reset();
            m_dummy.callEndpointRemoved(endpoint);
            assertRemovedCallback(listener);
        }
        finally {
            listener.unregister();
        }
    }

    private void assertAddedCallback(AbstractBlockingEndpointListener<?> listener) throws Exception {
        if (!listener.awaitAdded(1, TimeUnit.SECONDS)) {
            fail("Added callback expected");
        }
    }

    private void assertRemovedCallback(AbstractBlockingEndpointListener<?> listener) throws Exception {
        if (!listener.awaitRemoved(1, TimeUnit.SECONDS)) {
            fail("Removed callback expected");
        }
    }

    private void assertRemovedAddedCallbacks(AbstractBlockingEndpointListener<?> listener) throws Exception {
        if (!listener.awaitRemoved(1, TimeUnit.SECONDS)) {
            fail("Removed callback expected");
        }
        if (!listener.awaitAdded(1, TimeUnit.SECONDS)) {
            fail("Added callback expected");
        }
    }

    private void assertModifiedCallback(AbstractBlockingEndpointListener<?> listener) throws Exception {
        if (!listener.awaitModified(1, TimeUnit.SECONDS)) {
            fail("Modified callback expected");
        }
    }

    private void assertEndmatched(AbstractBlockingEndpointListener<?> listener) throws Exception {
        if (!listener.awaitEndmatch(1, TimeUnit.SECONDS)) {
            fail("Endmatch callback expected");
        }
    }
}
