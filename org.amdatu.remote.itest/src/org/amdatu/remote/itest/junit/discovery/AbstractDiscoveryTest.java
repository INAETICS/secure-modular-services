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
package org.amdatu.remote.itest.junit.discovery;

import static org.amdatu.remote.itest.util.ITestUtil.createEndpointDescription;

import java.util.concurrent.TimeUnit;

import org.amdatu.remote.itest.junit.RemoteServicesTestBase;
import org.amdatu.remote.itest.util.BlockingEndpointEventListener;
import org.amdatu.remote.itest.util.BlockingEndpointListener;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Abstract test for testing discovery implementations in isolation.
 * 
 * @param <T> The type of the registration key
 */
public abstract class AbstractDiscoveryTest<T> extends RemoteServicesTestBase {

    /**
     * Tests that a service (de-)registration is reflected by an endpoint description callback.
     * 
     * @throws Exception on failure
     */
    public void testServiceDiscovery() throws Exception {

        EndpointDescription endpointDescription = createEndpointDescription("q123", "interface2", 42l);

        BlockingEndpointListener block1 =
            new BlockingEndpointListener(getChildContext("CHILD1").getBundleContext(), endpointDescription);
        block1.register();

        BlockingEndpointEventListener block2 =
            new BlockingEndpointEventListener(getChildContext("CHILD1").getBundleContext(), endpointDescription);
        block2.register();

        try {
            T registration = publishEndpoint(endpointDescription);
            // FIXME literal should be config propery
            if (!block1.awaitAdded(30, TimeUnit.SECONDS)) {
                fail("Did not receive added callback with matching endpointdescription within 10 seconds");
            }
            if (!block2.awaitAdded(30, TimeUnit.SECONDS)) {
                fail("Did not receive added callback with matching endpointdescription within 10 seconds");
            }
            getParentContext().getLogService().log(LogService.LOG_DEBUG,
                "Received added callback for matching endpointdesciption!");

            block1.reset();
            block2.reset();
            revokeEndpoint(registration);
            // FIXME literal should be config propery
            if (!block1.awaitRemoved(30, TimeUnit.SECONDS)) {
                fail("Did not receive removed callback with matching endpointdescription within 10 seconds");
            }
            if (!block2.awaitRemoved(30, TimeUnit.SECONDS)) {
                fail("Did not receive removed callback with matching endpointdescription within 10 seconds");
            }
            getParentContext().getLogService().log(LogService.LOG_DEBUG,
                "Received removed callback for matching endpointdesciption!");
        }
        finally {
            block1.unregister();
            block2.unregister();
        }
    }

    /**
     * Publish an Endpoint Description in discovery.
     * 
     * @param endpoint The Endpoint Description
     * @return A registration key
     * @throws Exception If publishing fails
     */
    protected abstract T publishEndpoint(EndpointDescription endpoint) throws Exception;

    /**
     * Revoke and Endpoint registration.
     * 
     * @param registration The registration
     * @throws Exception If revoking fails
     */
    protected abstract void revokeEndpoint(T registration) throws Exception;

}
