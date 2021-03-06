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
package org.amdatu.remote.admin.itest.api;

import java.util.List;

/**
 * Simple implementation of {@link EchoInterface}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class EchoImpl implements EchoInterface {

    @Override
    public String echo(String name) {
        return name;
    }

    @Override
    public EchoData echo1(EchoData data) {
        return data;
    }

    @Override
    public List<EchoData> echo2(List<EchoData> data) {
        return data;
    }
}
