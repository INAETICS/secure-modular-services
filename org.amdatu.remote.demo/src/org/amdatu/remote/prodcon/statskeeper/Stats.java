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
package org.amdatu.remote.prodcon.statskeeper;

/**
 * Provides stats on how consumers/producers are doing.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class Stats {
    private boolean m_provider;
    private long m_startTime;
    private long m_endTime;
    private long m_loadValue;
    private long m_interval;

    public Stats() {
    }

    private Stats(boolean provider, long startTime, long endTime, long loadValue, long interval) {
        m_provider = provider;
        m_startTime = startTime;
        m_endTime = endTime;
        m_loadValue = loadValue;
        m_interval = interval;
    }

    public static Stats createConsumerStats(long startTime, long consumed, long interval) {
        return new Stats(false, startTime, System.currentTimeMillis(), consumed, interval);
    }

    public static Stats createProviderStats(long startTime, long produced, long interval) {
        return new Stats(true, startTime, System.currentTimeMillis(), produced, interval);
    }

    /**
     * @return the time at which the collecting of statistics was end, or this snapshot was taken, as Epoch time.
     */
    public long getEndTime() {
        return m_endTime;
    }

    /**
     * @return the interval in which currently items are produced or consumed.
     */
    public long getInterval() {
        return m_interval;
    }

    /**
     * @return the number of items consumed or produced, >= 0.
     */
    public long getLoadValue() {
        return m_loadValue;
    }

    /**
     * @return the time at which the collecting of statistics was started, as Epoch time.
     */
    public long getStartTime() {
        return m_startTime;
    }

    /**
     * @return <code>true</code> if these statistics are provider-based, <code>false</code> otherwise.
     */
    public boolean isProvider() {
        return m_provider;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(long endTime) {
        m_endTime = endTime;
    }

    /**
     * @param interval the interval to set
     */
    public void setInterval(long interval) {
        m_interval = interval;
    }

    /**
     * @param loadValue the loadValue to set
     */
    public void setLoadValue(long loadValue) {
        m_loadValue = loadValue;
    }

    /**
     * @param provider the provider to set
     */
    public void setProvider(boolean provider) {
        m_provider = provider;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        m_startTime = startTime;
    }
}
