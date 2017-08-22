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
package org.amdatu.remote.demo.chat.impl;

import java.util.Properties;

import org.amdatu.remote.demo.chat.api.MessageReceiver;
import org.amdatu.remote.demo.chat.api.MessageSender;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        // Nop
    }

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        ChatClient instance = new ChatClient();

        String userName = context.getProperty("chat.name");
        String hostName = java.net.InetAddress.getLocalHost().getHostName();

        Properties props = new Properties();
        props.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, MessageReceiver.class.getName());
        props.put("chat.name", userName + "@" + hostName);

        manager.add(createComponent()
            .setInterface(new String[] { MessageSender.class.getName(), MessageReceiver.class.getName() }, props)
            .setImplementation(instance)
            .add(createServiceDependency()
                .setService(MessageReceiver.class)
                .setRequired(false)
                .setCallbacks("addReceiver", "removeReceiver")
            ));
    }
}
