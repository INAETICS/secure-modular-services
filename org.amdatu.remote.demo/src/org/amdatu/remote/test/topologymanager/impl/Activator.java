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
package org.amdatu.remote.test.topologymanager.impl;

import java.util.Properties;

import org.amdatu.remote.test.topologymanager.Test;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext arg0, DependencyManager manager)
        throws Exception {
        Properties props = new Properties();
        props.setProperty(RemoteConstants.SERVICE_EXPORTED_INTERFACES, Test.class.getName());
        // props.setProperty(".slp.scopes", "a,b,c");

        manager.add(createComponent().setImplementation(TestImpl.class).setInterface(Test.class.getName(), props));
    }

    @Override
    public void destroy(BundleContext arg0, DependencyManager arg1)
        throws Exception {

    }
}
