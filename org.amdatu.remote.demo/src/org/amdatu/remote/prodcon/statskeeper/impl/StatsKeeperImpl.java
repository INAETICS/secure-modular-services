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
package org.amdatu.remote.prodcon.statskeeper.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.amdatu.remote.prodcon.producer.Producer;
import org.amdatu.remote.prodcon.statskeeper.Stats;
import org.amdatu.remote.prodcon.statskeeper.StatsProvider;
import org.apache.felix.dm.Component;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Provides a {@link Producer} that endlessly produces workpackages.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class StatsKeeperImpl {
    private final Map<ServiceReference<?>, StatsProvider> m_statProviders;
    // Injected by Felix DM...
    private volatile LogService m_logService;
    // Locally managed...
    private volatile ScheduledExecutorService m_executor;

    public StatsKeeperImpl() {
        m_statProviders = new HashMap<ServiceReference<?>, StatsProvider>();
    }

    public void add(ServiceReference<?> serviceReg, StatsProvider provider) {
        m_logService.log(LogService.LOG_INFO, "Adding new stats provider: " + provider);
        synchronized (m_statProviders) {
            if (m_statProviders.put(serviceReg, provider) == null) {
                m_logService.log(LogService.LOG_INFO, "Stats provider added (" + m_statProviders.size() + ")");
            }
        }
    }

    public void remove(ServiceReference<?> serviceReg, StatsProvider provider) {
        m_logService.log(LogService.LOG_INFO, "Removing stats provider: " + provider);
        synchronized (m_statProviders) {
            if (m_statProviders.remove(serviceReg) != null) {
                m_logService.log(LogService.LOG_INFO, "Stats provider removed (" + m_statProviders.size() + ")");
            }
        }
    }

    final void runStats() {
        Stats providerStats = null;
        Map<String, Stats> consumerStats;

        synchronized (m_statProviders) {
            consumerStats = new HashMap<String, Stats>(m_statProviders.size());
            for (StatsProvider provider : m_statProviders.values()) {
                Stats stats = provider.getStats();
                if (stats.isProvider()) {
                    providerStats = stats;
                }
                else {
                    consumerStats.put(provider.getId(), stats);
                }
            }
        }

        if (consumerStats.isEmpty()) {
            // Nothing to be done...
            return;
        }

        double lambda = 0.0;
        double muTotal = 0.0;
        long totalProduced = 0;
        long totalConsumed = 0;

        if (providerStats != null) {
            totalProduced = providerStats.getLoadValue();
            lambda = 1000.0 / providerStats.getInterval();

            System.out.printf("Provider produces %.2f workpackages/seconds.%n", lambda);
        }
        for (String id : consumerStats.keySet()) {
            Stats stats = consumerStats.get(id);
            totalConsumed += stats.getLoadValue();

            double mu = 1000.0 / stats.getInterval();
            muTotal += mu;

            System.out.printf("Consumer %s consumes %.2f workpackages/seconds.%n", id, mu);
        }

        long enqueued = totalProduced - totalConsumed;
        double rho = lambda / muTotal;

        System.out.printf("Provider queue size is %d workpackages (intensity = %f).%n%n", enqueued, rho);
    }

    protected void start(Component component) {
        m_executor = Executors.newScheduledThreadPool(1);
        m_executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    runStats();
                }
                catch (RuntimeException e) {
                    m_logService.log(LogService.LOG_WARNING, "Exception caught while collection stats!", e);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);

        m_logService.log(LogService.LOG_INFO, "StatsKeeper started...");
    }

    protected void stop(Component component) throws Exception {
        if (m_executor != null) {
            m_executor.shutdown();
            m_executor.awaitTermination(2, TimeUnit.SECONDS);
            m_executor = null;

            m_logService.log(LogService.LOG_INFO, "StatsKeeper stopped...");
        }
    }
}
