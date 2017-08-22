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
package org.amdatu.remote.prodcon.consumer.impl;

import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.amdatu.remote.prodcon.consumer.Consumer;
import org.amdatu.remote.prodcon.producer.Producer;
import org.amdatu.remote.prodcon.statskeeper.StatsProvider;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * Provides a factory for creating {@link BusyConsumer}s.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class ConsumerFactory {
    private final ConcurrentMap<String, Component> m_components;
    // Injected by Felix DM...
    private volatile BundleContext m_context;

    /**
     * Creates a new {@link ConsumerFactory} instance.
     */
    public ConsumerFactory() {
        m_components = new ConcurrentHashMap<String, Component>();
    }

    /**
     * GoGo command to add a new consumer.
     * 
     * @param session the session to use;
     * @param name the name of the consumer to add.
     */
    public void add(CommandSession session, String name) {
        if (name == null || "".equals(name.trim())) {
            throw new IllegalArgumentException("Name cannot be null or empty!");
        }

        String[] ifaces = new String[] { StatsProvider.class.getName() };

        Properties props = new Properties();
        props.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, ifaces);
        props.put("consumer.id", name);

        DependencyManager dm = new DependencyManager(m_context);
        Component comp = dm.createComponent()
            .setInterface(ifaces, props)
            .setImplementation(BusyConsumer.class)
            .add(dm.createServiceDependency()
                .setService(Producer.class)
                .setRequired(true))
            .add(dm.createServiceDependency()
                .setService(LogService.class)
                .setRequired(false));

        if (m_components.putIfAbsent(name, comp) == null) {
            dm.add(comp);

            session.getConsole().printf("Consumer '%s' added...%n", name);
        }
    }

    /**
     * GoGo command to list the existing consumers.
     * 
     * @param session the session to use.
     */
    public void list(CommandSession session) {
        PrintStream console = session.getConsole();

        console.printf("Current consumers:%n");
        for (Entry<String, Component> entry : m_components.entrySet()) {
            Consumer consumer = (Consumer) entry.getValue().getInstance();
            console.printf("%s: %s;%n", consumer.getId(), entry.getKey());
        }
    }

    /**
     * GoGo command to remove an existing consumer.
     * 
     * @param session the session to use;
     * @param name the name of the consumer to remove.
     */
    public void remove(CommandSession session, String name) {
        if (name == null || "".equals(name.trim())) {
            throw new IllegalArgumentException("Name cannot be null or empty!");
        }

        Component comp = m_components.remove(name);
        if (comp != null) {
            DependencyManager dm = comp.getDependencyManager();
            dm.remove(comp);

            session.getConsole().printf("Consumer '%s' removed...%n", name);
        }
    }
}
