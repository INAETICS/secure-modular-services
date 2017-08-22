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

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.amdatu.remote.prodcon.consumer.Consumer;
import org.amdatu.remote.prodcon.producer.Producer;
import org.amdatu.remote.prodcon.producer.WorkPackage;
import org.amdatu.remote.prodcon.statskeeper.Stats;
import org.apache.felix.dm.Component;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Provides an implementation of {@link Consumer} that performs a busy-wait loop to process work packages.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class BusyConsumer implements Consumer {
    private final AtomicLong m_consumed;
    // Injected by Felix DM...
    private volatile BundleContext m_context;
    private volatile Producer m_producer;
    private volatile LogService m_logService;
    // Locally managed...
    private volatile String m_id;
    private volatile ScheduledExecutorService m_executor;
    private volatile long m_fixedDelay;
    private volatile long m_startTime;

    public BusyConsumer() {
        m_consumed = new AtomicLong(0);
    }

    @Override
    public String getId() {
        return m_id;
    }

    @Override
    public Stats getStats() {
        return Stats.createConsumerStats(m_startTime, m_consumed.get(), m_fixedDelay);
    }

    final void runConsumer() {
        WorkPackage pkg = m_producer.getWorkPackage();
        if (pkg != null) {
            m_consumed.incrementAndGet();

            m_logService.log(LogService.LOG_INFO, "Consumed work package #" + pkg.getId());
        }
    }

    protected void start(Component component) throws Exception {
        m_id = getId(component);

        Random rnd = new Random();
        // pick a delay between 400 and 800 msec.
        m_fixedDelay = 600 + (rnd.nextInt(400) - 200);

        m_executor = Executors.newScheduledThreadPool(1);
        m_executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    runConsumer();
                }
                catch (RuntimeException e) {
                    m_logService.log(LogService.LOG_WARNING, "Exception caught while consuming work packages!", e);
                }
            }
        }, 100, m_fixedDelay, TimeUnit.MILLISECONDS);

        m_startTime = System.currentTimeMillis();
        m_consumed.set(0);

        m_logService.log(LogService.LOG_INFO, "Consumer " + m_id + " started at an interval of " + m_fixedDelay + " ms.");
    }

    protected void stop(Component component) throws Exception {
        if (m_executor != null) {
            m_executor.shutdown();
            m_executor.awaitTermination(1, TimeUnit.SECONDS);
            m_executor = null;

            m_logService.log(LogService.LOG_INFO, "Consumer " + m_id + " stopped...");
        }
    }

    private String getId(Component component) {
        String result = (String) component.getServiceProperties().get("consumer.id");
        if (result == null) {
            result = m_context.getProperty("consumer.id");
        }
        return result;
    }
}
