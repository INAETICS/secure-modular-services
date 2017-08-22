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
package org.amdatu.remote.admin.http;

import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Jackson wrapper for exceptions.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@JsonDeserialize(using = ExceptionWrapper.ExceptionDeserializer.class)
@JsonSerialize(using = ExceptionWrapper.ExceptionSerializer.class)
public class ExceptionWrapper {

    public static class ExceptionSerializer extends JsonSerializer<ExceptionWrapper> {

        @Override
        public void serialize(ExceptionWrapper obj, JsonGenerator gen, SerializerProvider sp) throws IOException {

            Throwable ex = obj.m_exception;
            gen.writeStartObject();
            gen.writeStringField("type", ex.getClass().getName());
            gen.writeStringField("msg", ex.getMessage());
            gen.writeObjectField("stacktrace", ex.getStackTrace());
            gen.writeEndObject();
        }
    }

    public static class ExceptionDeserializer extends JsonDeserializer<ExceptionWrapper> {

        private static final ObjectCodec CODEC = new ObjectMapper();

        @Override
        public ExceptionWrapper deserialize(JsonParser parser, DeserializationContext context) throws IOException {

            if (parser.getCodec() == null) {
                parser.setCodec(CODEC);
            }
            JsonNode node = parser.readValueAsTree();
            String typeName = node.get("type").textValue();
            String msg = node.get("msg").textValue();
            ArrayNode stackTraceNodes = (ArrayNode) node.get("stacktrace");
            Throwable t;

            try {
                Class<?> type;
                Bundle b = FrameworkUtil.getBundle(getClass());
                if (b != null) {
                    type = b.loadClass(typeName);
                }
                else {
                    type = getClass().getClassLoader().loadClass(typeName);
                }

                try {
                    // Try to create one with a message...
                    t = (Throwable) type.getConstructor(String.class).newInstance(msg);
                }
                catch (NoSuchMethodException e) {
                    // Try to create using the default constructor...
                    t = (Throwable) type.newInstance();
                }

                StackTraceElement[] stacktrace = new StackTraceElement[stackTraceNodes.size()];
                for (int i = 0; i < stacktrace.length; i++) {
                    stacktrace[i] = parser.getCodec().treeToValue(stackTraceNodes.get(i), StackTraceElement.class);
                }
                t.setStackTrace(stacktrace);
            }
            catch (Exception e) {
                e.printStackTrace();
                t = new RuntimeException(msg);
            }

            return new ExceptionWrapper(t);
        }
    }

    private final Throwable m_exception;

    public ExceptionWrapper(Throwable e) {
        m_exception = e;
    }

    public Throwable getException() {
        return m_exception;
    }
}
