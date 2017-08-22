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
package org.amdatu.remote.remotetest.topologymanager.impl;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.amdatu.remote.test.topologymanager.Test;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class TestConsumer implements Runnable {

    private volatile Thread thread = null;
    private ConcurrentMap<ServiceReference<Test>, Test> m_tests = new ConcurrentHashMap<ServiceReference<Test>, Test>();

    public TestConsumer() {
    }

    public void start() {
        thread = new Thread(this, "TestConsumer Thread");
        thread.start();
    }

    public void stop() {
        thread = null;
    }

    public void add(ServiceReference<Test> reference, Test test) {
        m_tests.put(reference, test);
    }

    public void remove(ServiceReference<Test> reference, Test test) {
        m_tests.remove(reference);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                for (Test test : m_tests.values()) {
                    test.printMessage("It is " + new Date() + " on the consumer side");
                }

                try {
                    Thread.sleep(10000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
