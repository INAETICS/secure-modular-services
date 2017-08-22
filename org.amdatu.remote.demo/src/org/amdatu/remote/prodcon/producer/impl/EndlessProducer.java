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
package org.amdatu.remote.prodcon.producer.impl;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.amdatu.remote.prodcon.producer.Producer;
import org.amdatu.remote.prodcon.producer.WorkPackage;
import org.amdatu.remote.prodcon.statskeeper.Stats;
import org.apache.felix.dm.Component;
import org.osgi.service.log.LogService;

/**
 * Provides a {@link Producer} that endlessly produces workpackages.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class EndlessProducer implements Producer {
    private final BlockingQueue<WorkPackage> m_queue;
    private final AtomicLong m_id;
    private final AtomicLong m_produced;

    // Injected by Felix DM...
    private volatile LogService m_logService;
    // Locally managed
    private volatile long m_interval;
    private volatile long m_startTime;
    private volatile ScheduledExecutorService m_executor;

    /**
     * Creates a new {@link EndlessProducer} instance.
     */
    public EndlessProducer() {
        m_id = new AtomicLong();
        m_produced = new AtomicLong();

        m_queue = new LinkedBlockingQueue<WorkPackage>();
    }

    @Override
    public String getId() {
        return getClass().getSimpleName();
    }

    @Override
    public Stats getStats() {
        return Stats.createProviderStats(m_startTime, m_produced.get(), m_interval);
    }

    @Override
    public WorkPackage getWorkPackage() {
        return m_queue.poll();
    }

    final void runProducer() {
        m_queue.add(new WorkPackage(m_id.incrementAndGet()));

        m_produced.incrementAndGet();
    }

    protected void start(Component component) throws Exception {
        m_executor = Executors.newScheduledThreadPool(1);

        Random rnd = new Random();
        // pick a delay between 300 and 700 msec.
        m_interval = 500 + (rnd.nextInt(400) - 200);

        m_executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    runProducer();
                }
                catch (RuntimeException e) {
                    m_logService.log(LogService.LOG_WARNING, "Exception caught while producing work packages!", e);
                }
            }
        }, 100, m_interval, TimeUnit.MILLISECONDS);

        m_produced.set(0);
        m_startTime = System.currentTimeMillis();

        m_logService.log(LogService.LOG_INFO, "Endless producer started at an interval of " + m_interval + " ms.");
    }

    protected void stop(Component component) throws Exception {
        if (m_executor != null) {
            m_executor.shutdown();
            m_executor.awaitTermination(2, TimeUnit.SECONDS);
            m_executor = null;

            m_logService.log(LogService.LOG_INFO, "Endless producer stopped...");
        }
    }
}
