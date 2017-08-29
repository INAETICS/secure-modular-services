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
package org.amdatu.remote.demo.inaetics.resolver.impl;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * @author SudoHenk
 */
public class Activator extends DependencyActivatorBase {
    public static final String INAETICS_ATTRS_UNIVERSE = "inaetics.attrs_univ";
    
    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        // Nop
    }

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        String storagedir = System.getProperty("user.dir") + "\\resources\\tmp\\";
        String curveparamsFileLocation = System.getProperty("user.dir") + "\\resources\\curveparams.txt";
        String[] attrs_univ = context.getProperty(INAETICS_ATTRS_UNIVERSE).split(";");
        Resolver res = new Resolver();
        res.setup(storagedir, curveparamsFileLocation, attrs_univ);
        res.startSolution();
    }
}
