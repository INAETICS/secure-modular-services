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

import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Provides the bundle activator.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class Activator extends DependencyActivatorBase {

    @Override
    public void destroy(BundleContext context, DependencyManager dm) throws Exception {
        // Nop
    }

    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception {
        Properties props = new Properties();
        props.put(CommandProcessor.COMMAND_SCOPE, "prodcon");
        props.put(CommandProcessor.COMMAND_FUNCTION, new String[]{ "add", "remove", "list" });

        dm.add(createComponent()
            .setInterface(Object.class.getName(), props)
            .setImplementation(ConsumerFactory.class)
            .add(createServiceDependency()
                .setService(LogService.class)
                .setRequired(false))
            );
    }
}
