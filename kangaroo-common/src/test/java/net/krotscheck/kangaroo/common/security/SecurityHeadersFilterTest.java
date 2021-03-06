/*
 * Copyright (c) 2017 Michael Krotscheck
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
 *
 */

package net.krotscheck.kangaroo.common.security;

import com.google.common.net.HttpHeaders;
import net.krotscheck.kangaroo.test.jersey.KangarooJerseyTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test that all the expected filters are attached.
 *
 * @author Michael Krotscheck
 */
public final class SecurityHeadersFilterTest extends KangarooJerseyTest {

    /**
     * Build an application.
     *
     * @return A configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        ResourceConfig config = new ResourceConfig();
        config.register(new SecurityHeadersFilter.Binder());
        config.register(MockService.class);
        return config;
    }

    /**
     * Assert that the jackson feature is available.
     */
    @Test
    public void testFilters() {
        Response r = target("/").request().get();

        // We expect 4 headers, 3 provided by the underlying framework, and
        // one provided by the filter.
        MultivaluedMap<String, Object> headers = r.getHeaders();

        assertEquals(3, headers.size());

        // Framework provided tests.
        assertNotNull(headers.get(HttpHeaders.CONTENT_LENGTH));

        // Expected headers.
        assertEquals("Deny",
                headers.getFirst(HttpHeaders.X_FRAME_OPTIONS));
    }

    /**
     * A simple endpoint.
     *
     * @author Michael Krotscheck
     */
    @Path("/")
    public static final class MockService {

        /**
         * Return OK.
         *
         * @return Nothing, error thrown.
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response modifyPojo() {
            return Response.ok().build();
        }

    }
}
