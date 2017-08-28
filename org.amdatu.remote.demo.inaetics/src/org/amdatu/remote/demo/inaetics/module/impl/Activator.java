package org.amdatu.remote.demo.inaetics.module.impl;

import java.util.Properties;

import org.amdatu.remote.demo.inaetics.module.api.MessageReceiver;
import org.amdatu.remote.demo.inaetics.module.api.MessageSender;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * @author SudoHenk
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
