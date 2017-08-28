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
package org.amdatu.remote.demo.inaetics.module.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.amdatu.remote.demo.inaetics.module.api.MessageReceiver;
import org.amdatu.remote.demo.inaetics.module.api.MessageSender;
import org.apache.felix.dm.Component;
import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class ChatClient implements MessageSender, MessageReceiver {
    private final List<ChatPeer> m_peers = new LinkedList<ChatPeer>();

    private volatile BundleContext m_context;
    private volatile ServiceRegistration<?> m_serviceReg;
    private volatile String m_name;

    @Override
    public void receive(String sender, String message) {
        if (message.length() < 2) {
            throw new IllegalArgumentException("Too short!");
        }
        System.out.printf("%s> %s%n", sender, message);
    }

    @Override
    public void send(String message) {
        List<ChatPeer> peers;
        synchronized (m_peers) {
            peers = new ArrayList<ChatPeer>(m_peers);
        }

        System.out.printf("you> %s%n", message);

        for (ChatPeer peer : peers) {
            peer.getMessageReceiver().receive(m_name, message);
        }
    }

    void init(Component comp) throws Exception {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(CommandProcessor.COMMAND_SCOPE, "chat");
        props.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "send" });

        // Register the Gogo commands separately, as to avoid the registration of the endpoints to cause havoc in Gogo...
        m_serviceReg = m_context.registerService(Object.class.getName(), this, props);

        m_name = (String) comp.getServiceProperties().get("chat.name");
        System.out.println("Starting " + m_name);
    }

    void stop(Component comp) throws Exception {
        if (m_serviceReg != null) {
            m_serviceReg.unregister();
            m_serviceReg = null;
        }
        System.out.println("Stopping " + m_name);
    }

    void addReceiver(ServiceReference<MessageReceiver> reference,
        MessageReceiver receiver) {
        if (receiver == this) {
            return;
        }

        String chatName = (String) reference.getProperty("chat.name");
        if (chatName == null || "".equals(chatName)) {
            chatName = "peer " + reference.getProperty(Constants.SERVICE_ID);
        }

        synchronized (m_peers) {
            m_peers.add(new ChatPeer(chatName, receiver));
        }
        System.out.println("> " + chatName + " joined the room");

        receiver.receive("room-bot", "Hello, " + chatName + " from " + m_name
            + "!");
    }

    void removeReceiver(MessageReceiver receiver) {
        if (receiver == this) {
            return;
        }

        ChatPeer peer = null;
        synchronized (m_peers) {
            for (ChatPeer chatpeer : m_peers) {
                if (chatpeer.getMessageReceiver() == receiver) {
                    peer = chatpeer;
                    break;
                }
            }
            if (peer != null)
                m_peers.remove(peer);
        }

        if (peer != null) {
            receiver.receive("room-bot", "You (" + peer.getName() + ") left the room...");
        }
    }

    static class ChatPeer {
        private final String m_name;
        private final MessageReceiver m_receiver;

        public ChatPeer(String name, MessageReceiver receiver) {
            m_name = name;
            m_receiver = receiver;
        }

        public String getName() {
            return m_name;
        }

        public MessageReceiver getMessageReceiver() {
            return m_receiver;
        }
    }
}
