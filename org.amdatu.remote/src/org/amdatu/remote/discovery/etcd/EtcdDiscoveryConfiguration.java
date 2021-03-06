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
package org.amdatu.remote.discovery.etcd;

import org.amdatu.remote.discovery.HttpEndpointDiscoveryConfiguration;

/**
 * Interface for accessing etcd discovery configuration values.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface EtcdDiscoveryConfiguration extends HttpEndpointDiscoveryConfiguration {

    /**
     * returns the connect url for the etcd discovery
     * 
     * @return the connect url
     */
    public String getConnectUrl();

    /**
     * returns the root path for the etcd discovery
     * 
     * @return the root path
     */
    public String getRootPath();
}
