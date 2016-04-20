/*
 * Copyright (c) 2016 Michael Krotscheck
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.krotscheck.features.jackson;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This feature collects all custom serializers and deserializers in the
 * injection scope, and builds a jackson object mapper that may be used anywhere
 * within the system.
 */
public final class JacksonFeature implements Feature {

    /**
     * Register this feature.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        // Pull in the Jackson provider, but not the exception mappers.
        context.register(JacksonJaxbJsonProvider.class);
        
        // Jackson configuration.
        context.register(new JacksonSerializerModule.Binder());
        context.register(new ObjectMapperFactory.Binder());

        return true;
    }
}
