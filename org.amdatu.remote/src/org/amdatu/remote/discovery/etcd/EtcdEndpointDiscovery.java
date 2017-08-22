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


import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.amdatu.remote.discovery.AbstractHttpEndpointDiscovery;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

import mousio.client.promises.ResponsePromise;
import mousio.client.promises.ResponsePromise.IsSimplePromiseResponseHandler;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdKeyAction;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;
import nl.sudohenk.kpabe.KeyPolicyAttributeBasedEncryption;

/**
 * Etcd implementation of service endpoint based discovery. This type of discovery discovers HTTP endpoints
 * that provide published services based on the {@link EndpointDescription} extender format.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class EtcdEndpointDiscovery extends AbstractHttpEndpointDiscovery<EtcdDiscoveryConfiguration> {

    public static final String DISCOVERY_NAME = "Amdatu Remote Service Endpoint (Etcd)";
    public static final String DISCOVERY_TYPE = "etcd";

    private volatile ScheduledExecutorService m_initExecutor;
    private volatile ScheduledExecutorService m_updateExecutor;
    private volatile ResponseListener m_responseListener;

    private volatile EtcdRegistrationUpdater m_updater;
    private volatile EtcdClient m_etcd;

    public EtcdEndpointDiscovery(EtcdDiscoveryConfiguration configuration) {
        super(DISCOVERY_TYPE, configuration);
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();

        m_updateExecutor = Executors.newSingleThreadScheduledExecutor();

        m_responseListener = new ResponseListener();

        logDebug("Connecting to %s", getConfiguration().getConnectUrl());
        m_etcd = new EtcdClient(URI.create(getConfiguration().getConnectUrl()));
        logDebug("Etcd version is %s", m_etcd.getVersion());
        m_updater = new EtcdRegistrationUpdater();
        initDiscoveryEndpoints();
    }

    @Override
    protected void stopComponent() throws Exception {
        try {
            m_updater.cancel();
        }
        catch (Exception e) {
            logError("Cancel updater failed", e);
        }
        m_updater = null;

        try {
            m_etcd.close();
        }
        catch (Exception e) {
            logError("Closing etcd client failed", e);
        }
        m_etcd = null;

        try {
            m_initExecutor.shutdown();
            if (!m_initExecutor.awaitTermination(1l, TimeUnit.SECONDS)) {
                m_initExecutor.shutdownNow();
            }
        }
        catch (Exception e) {
            logWarning("Exception while stopping init executer", e);
        }
        m_initExecutor = null;

        try {
            m_updateExecutor.shutdown();
            if (!m_updateExecutor.awaitTermination(1l, TimeUnit.SECONDS)) {
                m_updateExecutor.shutdownNow();
            }
        }
        catch (Exception e) {
            logWarning("Exception while stopping update executer", e);
        }
        m_updateExecutor = null;

        super.stopComponent();
    }

    private void initDiscoveryEndpoints() {
        // in case of recursive calls shut a running executor down
        if (m_initExecutor != null && !m_initExecutor.isShutdown()) {
            m_initExecutor.shutdownNow();
        }
        m_initExecutor = Executors.newSingleThreadScheduledExecutor();
        m_initExecutor.execute(new InitTask());
    }

    private class InitTask implements Runnable {
        @Override
        public void run() {
            try {
                initDiscoveryEndpointsInternal();
            }
            catch (EtcdWatchException e) {
                logError("Could not set watch for init discovery endpoints, retrying...", e);
                m_initExecutor.schedule(this, 1, TimeUnit.SECONDS);
            }
        }
    }

    private void initDiscoveryEndpointsInternal() throws EtcdWatchException {
        long index = 0l;
        try {
            EtcdKeysResponse response = m_etcd.getDir(getConfiguration().getRootPath()).send().get();
            index = getEtcdIndex(response);
            logDebug("Initializing peer endpoints at etcd index %s", index);
            Map<String, URL> endpoints = new HashMap<String, URL>();
            if (response.node.dir && response.node.nodes != null) {
                for (EtcdNode node : response.node.nodes) {
                    if (node.key.endsWith(getLocalNodePath())) {
                        // ignore ourself
                        logDebug("Skipping %s", node.key);
                        continue;
                    }
                    try {
                        logDebug("Adding %s", node.key);
                        endpoints.put(node.key, new URL(node.value));
                    }
                    catch (Exception e) {
                        logWarning("Failed to add discovery endpoint", e);
                    }
                }
            }
            setDiscoveryEndpoints(endpoints);
        }
        catch (Throwable e) {
            logError("Could not initialize peer discovery endpoints!", e);
        }
        finally {
            setDirectoryWatch(index + 1);
        }
    }

    private void handleDiscoveryEndpointChange(EtcdKeysResponse response) throws EtcdWatchException {
        long index = 0l;
        try {
            index = response.node.modifiedIndex;
            logDebug("Handling peer endpoint change at etcd index %s", index);
            if (!response.node.key.endsWith(getLocalNodePath())) {
                // we get "set" on a watch response
                if (response.action == EtcdKeyAction.set) {

                    // when its changed, first remove old endpoint
                    if (response.prevNode != null && !response.prevNode.value.equals(response.node.value)) {
                        removeDiscoveryEndpoint(response.prevNode.key);
                    }

                    // when it's new or changed, add endpoint
                    if (response.prevNode == null || !response.prevNode.value.equals(response.node.value)) {
                        addDiscoveryEndpoint(response.node.key, new URL(response.node.value));
                    }
                }
                // remove endpoint on "delete" or "expire", and it's not about ourself
                else if ((response.action == EtcdKeyAction.delete || response.action == EtcdKeyAction.expire)
                    && !response.prevNode.value.equals(getConfiguration().getBaseUrl())) {
                    removeDiscoveryEndpoint(response.prevNode.key);
                }
            }
        }
        catch (Exception e) {
            logError("Could not handle peer discovery endpoint change!", e);
        }
        finally {
            setDirectoryWatch(index + 1);
        }
    }

    private long getEtcdIndex(EtcdKeysResponse response) {

        long index = 0l;
        if (response != null) {
            // get etcdIndex with fallback to modifiedIndex
            // see https://github.com/coreos/etcd/pull/1082#issuecomment-56444616
            if (response.etcdIndex != null) {
                index = response.etcdIndex;
            }
            else if (response.node.modifiedIndex != null) {
                index = response.node.modifiedIndex;
            }
            // potential bug fallback
            // see https://groups.google.com/forum/?hl=en#!topic/etcd-dev/S12405PCKaU
            if (response.node.dir && response.node.nodes != null) {
                for (EtcdNode node : response.node.nodes) {
                    if (node.modifiedIndex > index) {
                        index = node.modifiedIndex;
                    }
                }
            }
        }
        return index;
    }

    private void setDirectoryWatch(long index) throws EtcdWatchException {

        logDebug("Setting watch for index %s", index);

        try {
            m_etcd.get(getConfiguration().getRootPath())
                .waitForChange((int) index)
                .recursive()
                .send()
                .addListener(m_responseListener);
        }
        catch (Throwable e) {
            logWarning("Could not set etcd watch!", e);
            // we can't do anything useful here, let the caller decide what to do
            throw new EtcdWatchException(e);
        }

    }

    private String getLocalNodePath() {
        return getNodePath(getFrameworkUUID());
    }

    private String getNodePath(String nodeID) {
        String path = getConfiguration().getRootPath();
        if (path.endsWith("/")) {
            return path + nodeID;
        }
        return path + "/" + nodeID;
    }

    private class EtcdRegistrationUpdater implements Runnable {

        private static final int ETCD_REGISTRATION_TTL = 60;

        private final ScheduledFuture<?> m_future;

        public EtcdRegistrationUpdater() throws Exception {
            m_future =
                m_updateExecutor.scheduleAtFixedRate(this, 0, ETCD_REGISTRATION_TTL - 5,
                    TimeUnit.SECONDS);
        }

        @Override
        public void run() {
            try {
                // ADDON
                String test_folder = "C://abeproject/";
                String curveparamsFileLocation = test_folder + "curveparams";
                
                KeyPolicyAttributeBasedEncryption kpabe = new KeyPolicyAttributeBasedEncryption();
//                String pubfile = test_folder + "publickey";
//                String mskfile = test_folder + "mastersecretkey";
//                String[] attrs_univ = {"application1", "module1", "solution1"};
//                kpabe.setup(pubfile, mskfile, attrs_univ, curveparamsFileLocation);
                
                EtcdResponsePromise<EtcdKeysResponse> responsePromise =
                    m_etcd.put(getLocalNodePath(), getConfiguration().getBaseUrl().toExternalForm())
                        .ttl(ETCD_REGISTRATION_TTL).send();
                logDebug("registered at etcd index " + responsePromise.get().etcdIndex);
            }
            catch (Exception e) {
                logError("Etcd registration update failed", e);
            }
        }

        public void cancel() {
            try {
                m_future.cancel(false);
                m_etcd.delete(getLocalNodePath()).send().get();
            }
            catch (Exception e) {
                logError("Etcd deregistration update failed", e);
            }
        }
    }

    private class ResponseListener implements IsSimplePromiseResponseHandler<EtcdKeysResponse> {

        @Override
        public void onResponse(ResponsePromise<EtcdKeysResponse> promise) {
            logDebug("watch was triggered");
            try {
                if (promise.getException() != null) {
                    logWarning("etcd watch received exception: %s", promise.getException().getMessage());
                    initDiscoveryEndpoints();
                    return;
                }
                handleDiscoveryEndpointChange(promise.get());
            }
            catch (Exception e) {
                logWarning("Could not handle discovery endpoint change or set a new watch", e);
                initDiscoveryEndpoints();
            }
        }
    }
}
