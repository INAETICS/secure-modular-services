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
package org.amdatu.remote;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.SAXParserFactory;

import org.amdatu.remote.discovery.HttpEndpointDiscoveryConfiguration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author Sudohenk
 *
 */
public final class SecureEndpointDescriptorWriter {

    private final static SAXParserFactory SAX_PARSERFACTORY = SAXParserFactory.newInstance();

    public void writeDocument(Writer writer, HttpEndpointDiscoveryConfiguration m_configuration, EndpointDescription... endpoints) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<endpoint-descriptions xmlns=\"http://www.osgi.org/xmlns/rsa/v1.0.0\">\n");
        for (EndpointDescription endpoint : endpoints) {
            appendEndpoint(sb, endpoint);
        }
        sb.append("</endpoint-descriptions>");
        // write to response
        writer.write(m_configuration.encrypt(sb.toString()));
    }

    private static void appendEndpoint(StringBuilder sb, EndpointDescription endpoint) throws IOException {
        sb.append("  <endpoint-description>\n");
        for (Entry<String, Object> entry : endpoint.getProperties().entrySet()) {
            appendProperty(sb, entry.getKey(), entry.getValue());
        }
        sb.append("  </endpoint-description>\n");
    }

    private static void appendProperty(StringBuilder sb, String key, Object value) throws IOException {
        if (value.getClass().isArray() || value instanceof List<?> || value instanceof Set<?>) {
            appendMultiValueProperty(sb, key, value);
        }
        else {
            appendSingleValueProperty(sb, key, value);
        }
    }

    private static void appendSingleValueProperty(StringBuilder sb, String key, Object value) throws IOException {
        if (ValueTypes.get(value.getClass()) == null) {
            throw new IllegalStateException("Unsupported type : " + value.getClass());
        }
        if (value instanceof String) {
            String string = (String) value;
            if (string.trim().startsWith("<") && isWellFormedXml(string)) {
                sb.append("    <property name=\"").append(escapeXml(key)).append("\">\n").append("      <xml>")
                    .append((String) value).append("</xml>\n").append("    </property>\n");
            }
            else {
                sb.append("   <property name=\"").append(escapeXml(key)).append("\" value=\"")
                    .append(escapeXml(string)).append("\"/>\n");
            }
        }
        else {
            sb.append("   <property name=\"").append(escapeXml(key))
                .append("\" value-type=\"" + value.getClass().getSimpleName() + "\" value=\"")
                .append(escapeXml(value.toString())).append("\"/>\n");
        }
    }

    private static void appendMultiValueProperty(StringBuilder sb, String key, Object value) throws IOException {

        Class<?> componentType = determineComponentType(value);
        if (ValueTypes.get(componentType) == null) {
            throw new IllegalStateException();
        }
        if (componentType.equals(String.class)) {
            sb.append("   <property name=\"").append(escapeXml(key)).append("\">\n");
        }
        else {
            sb.append("   <property name=\"").append(escapeXml(key)).append("\" value-type=\"")
                .append(componentType.getSimpleName()).append("\">\n");
        }
        if (value.getClass().isArray()) {
            List<Object> objectList = new ArrayList<Object>();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                objectList.add(Array.get(value, i));
            }
            sb.append("      <array>").append("\n");
            appendMultiValues(sb, objectList);
            sb.append("      </array>\n");
        }
        else if (value instanceof List<?>) {
            sb.append("      <list>").append("\n");
            appendMultiValues(sb, (List<?>) value);
            sb.append("      </list>\n");
        }
        else if (value instanceof Set<?>) {
            sb.append("      <set>").append("\n");
            appendMultiValues(sb, (Set<?>) value);
            sb.append("      </set>\n");
        }
        sb.append("   </property>\n");
    }

    private static void appendMultiValues(StringBuilder sb, Collection<?> value) throws IOException {
        for (Iterator<?> it = value.iterator(); it.hasNext();) {
            sb.append("         <value>").append(escapeXml(it.next().toString())).append("</value>").append("\n");
        }
    }

    private static Class<?> determineComponentType(Object value) {
        if (value.getClass().isArray()) {
            return value.getClass().getComponentType();
        }
        if (value instanceof Collection<?>) {
            Collection<?> col = ((Collection<?>) value);
            if (col.isEmpty()) {
                return String.class;
            }
            else {
                return col.iterator().next().getClass();
            }
        }
        return value.getClass();
    }

    private static boolean isWellFormedXml(String value) {
        try {
            InputSource source = new InputSource(new StringReader(value));
            SAX_PARSERFACTORY.newSAXParser().parse(source, new DefaultHandler());
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}
