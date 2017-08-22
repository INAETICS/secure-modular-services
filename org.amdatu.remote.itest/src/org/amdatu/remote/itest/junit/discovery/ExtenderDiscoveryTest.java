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
package org.amdatu.remote.itest.junit.discovery;

import static org.amdatu.remote.itest.config.Configs.configs;
import static org.amdatu.remote.itest.config.Configs.frameworkConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.amdatu.remote.EndpointUtil;
import org.amdatu.remote.itest.config.Config;
import org.amdatu.remote.itest.config.FrameworkConfig;
import org.amdatu.remote.itest.util.FrameworkContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Tests Extender discovery.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class ExtenderDiscoveryTest extends AbstractDiscoveryTest<Bundle> {

    @Override
    protected Config[] configureFramework(FrameworkContext parent) throws Exception {

        String systemPackages = getParentContext().getBundleContext().getProperty("itest.systempackages");
        String defaultBundles = getParentContext().getBundleContext().getProperty("itest.bundles.default");
        String discoveryBundles = getParentContext().getBundleContext().getProperty("itest.bundles.discovery.extender");

        parent.setLogLevel(LogService.LOG_DEBUG);
        parent.setServiceTimout(30000);

        FrameworkConfig child1 = frameworkConfig("CHILD1")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(10000)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .bundlePaths(defaultBundles, discoveryBundles);

        return configs(child1);
    }

    @Override
    protected void configureServices() throws Exception {
    }

    @Override
    protected void cleanupTest() throws Exception {
    }

    @Override
    protected Bundle publishEndpoint(EndpointDescription endpoint) throws Exception {
        StringWriter writer = new StringWriter();
        EndpointUtil.writeEndpoints(writer, endpoint);

        String xml = writer.toString();

        String path = createExtenderBundle(xml);
        Bundle bundle = getChildContext("CHILD1").installBundles(path)[0];
        bundle.start();
        return bundle;
    }

    @Override
    protected void revokeEndpoint(Bundle bundle) throws Exception {
        bundle.stop();
    }

    // TODO Code below must be checked and may be a generic util functions

    private static String createExtenderBundle(String xmlStr) throws IOException {

        File bundle = File.createTempFile("extenderbundle", ".jar");

        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.put(new Attributes.Name(Constants.BUNDLE_SYMBOLICNAME), "testbundle." + System.currentTimeMillis());
        attrs.put(new Attributes.Name(Constants.BUNDLE_MANIFESTVERSION), "2");
        attrs.put(new Attributes.Name(Constants.BUNDLE_VERSION), "1.0.0");
        attrs.put(new Attributes.Name("Remote-Service"), "endpoint.xml");

        // the extender must publish the capability
        attrs.put(new Attributes.Name("Require-Capability"),
            "osgi.extender;filter:=\"(osgi.extender=osgi.remoteserviceadmin)\"");

        JarOutputStream out = new JarOutputStream(new FileOutputStream(bundle), manifest);
        JarEntry entry = new JarEntry("endpoint.xml");
        out.putNextEntry(entry);
        out.write(xmlStr.getBytes());
        out.flush();
        out.closeEntry();
        out.close();
        return bundle.getAbsolutePath();
    }
}
