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

package net.krotscheck.kangaroo.common.exception.mapper;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Exception mapper for errors that are otherwise not handled.
 *
 * @author Michael Krotscheck
 */
@Provider
public final class UnhandledExceptionMapper
        implements ExceptionMapper<Exception> {

    /**
     * Convert to response.
     *
     * @param exception The exception to convert.
     * @return A Response instance for this error.
     */
    public Response toResponse(final Exception exception) {
        return ErrorResponseBuilder
                .from(exception)
                .build();
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(UnhandledExceptionMapper.class)
                    .to(ExceptionMapper.class)
                    .in(Singleton.class);
        }
    }
}
