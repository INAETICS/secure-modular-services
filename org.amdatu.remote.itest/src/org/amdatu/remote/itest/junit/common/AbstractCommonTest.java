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
package org.amdatu.remote.itest.junit.common;

import static org.amdatu.remote.itest.config.Configs.configs;
import static org.amdatu.remote.itest.config.Configs.frameworkConfig;

import org.amdatu.remote.itest.config.Config;
import org.amdatu.remote.itest.config.FrameworkConfig;
import org.amdatu.remote.itest.junit.RemoteServicesTestBase;
import org.amdatu.remote.itest.util.FrameworkContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

/**
 * Base class for common integration tests. It simply starts a single child context and only provisionins the defaults.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractCommonTest extends RemoteServicesTestBase {

    @Override
    protected Config[] configureFramework(FrameworkContext parent) throws Exception {

        String systemPackages = getParentContext().getBundleContext().getProperty("itest.systempackages");
        String defaultBundles = getParentContext().getBundleContext().getProperty("itest.bundles.default");

        parent.setLogLevel(LogService.LOG_DEBUG);
        parent.setServiceTimout(30000);

        FrameworkConfig child1 = frameworkConfig("CHILD1")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(10000)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .bundlePaths(defaultBundles);

        return configs(child1);
    }

    @Override
    protected void configureServices() throws Exception {
    }

    @Override
    protected void cleanupTest() throws Exception {
    }

    /**
     * Quick access to the single child context.
     * 
     * @return the child context
     */
    protected FrameworkContext getChildContext() {
        return getChildContext("CHILD1");
    }
}
