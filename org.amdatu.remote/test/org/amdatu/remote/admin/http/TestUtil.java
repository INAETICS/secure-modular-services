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

import org.apache.avro.reflect.Nullable;
import org.apache.avro.reflect.Union;

/**
 * 
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class TestUtil {
    private static final Boolean DEFAULT_BOOLEAN = Boolean.FALSE;
    private static final Byte DEFAULT_BYTE = new Byte((byte) 0);
    private static final Short DEFAULT_SHORT = new Short((short) 0);
    private static final Integer DEFAULT_INT = new Integer(0);
    private static final Long DEFAULT_LONG = new Long(0);
    private static final Float DEFAULT_FLOAT = new Float(0.0f);
    private static final Double DEFAULT_DOUBLE = new Double(0.0);

    public static Object getDefaultValue(Class<?> returnType) {
        if (Boolean.class.equals(returnType) || Boolean.TYPE.equals(returnType)) {
            return DEFAULT_BOOLEAN;
        }
        else if (Byte.class.equals(returnType) || Byte.TYPE.equals(returnType)) {
            return DEFAULT_BYTE;
        }
        else if (Short.class.equals(returnType) || Short.TYPE.equals(returnType)) {
            return DEFAULT_SHORT;
        }
        else if (Integer.class.equals(returnType) || Integer.TYPE.equals(returnType)) {
            return DEFAULT_INT;
        }
        else if (Long.class.equals(returnType) || Long.TYPE.equals(returnType)) {
            return DEFAULT_LONG;
        }
        else if (Float.class.equals(returnType) || Float.TYPE.equals(returnType)) {
            return DEFAULT_FLOAT;
        }
        else if (Double.class.equals(returnType) || Double.TYPE.equals(returnType)) {
            return DEFAULT_DOUBLE;
        }
        else {
            return null;
        }
    }

    public static interface BoundType extends GenericType<Long, String> {
        String m(String y, Long x);
    }

    public static interface GenericType<X extends Number, Y> {
        void m(X x);

        X m(X x, Y y);

        int m(Y y);
    }
    
    public static interface AvroBoundType extends GenericType<Long, String> {
        String m3(String y, Long x);
    }

    /*
     * Interface used to test implementations that doesn't support method overloading/duplicated method names.
     */
    public static interface AvroGenericType<X extends Number, Y> {
        void m0(X x);

        X m1(X x, Y y);

        int m2(Y y);
    }

    /*
     * An interface to test Avro unions
     */
    public static interface AvroUnionType {
        @Union({ Integer.class, Double.class, Void.class })
        Number m(@Union({ Integer.class, Double.class, Void.class }) Number x, @Nullable String y);
    }

    public static interface ServiceA {
        void doException() throws IOException;

        void doNothing();

        int doubleIt(int value) throws IllegalArgumentException;
        
        @Nullable
        Object returnNull();

        int tripeIt(@Nullable Integer value) throws IllegalArgumentException;
    }

    public static class ServiceAImpl implements ServiceA {
        @Override
        public void doException() throws IOException {
            throw new IOException("Exception!");
        }

        @Override
        public void doNothing() {
            // Nop
        }

        @Override
        public int doubleIt(int value) throws IllegalArgumentException {
            if (value <= 0) {
                throw new IllegalArgumentException("Invalid value!");
            }
            return value + value;
        }

        @Override
        public Object returnNull() {
            return null;
        }

        @Override
        public int tripeIt(Integer value) throws IllegalArgumentException {
            return value != null ? value.intValue() * 3 : 0;
        }
    }

}
