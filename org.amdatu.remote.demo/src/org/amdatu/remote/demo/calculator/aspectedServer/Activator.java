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
package org.amdatu.remote.demo.calculator.aspectedServer;

import org.apache.celix.calc.api.Calculator;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class Activator extends DependencyActivatorBase implements Calculator {

    private volatile Calculator m_calculator;
    
    public double add(double a, double b) {
        System.out.printf("Hello from AspectService!\n");
        return m_calculator.add(a, b);
    }

    public double sub(double a, double b) {
        return m_calculator.sub(a, b);
    }

    public double sqrt(double a) {
        return m_calculator.sqrt(a);
    }

    public void init(BundleContext context, DependencyManager manager) throws Exception {
        manager.add(createAspectService(Calculator.class, null, 10).setImplementation(this));
    }

    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }
}
