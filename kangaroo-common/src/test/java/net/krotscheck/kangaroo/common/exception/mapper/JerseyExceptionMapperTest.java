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

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Test that jersey exceptions are caught and rewritten into appropriate
 * responses.
 *
 * @author Michael Krotscheck
 */
public final class JerseyExceptionMapperTest {

    /**
     * Test converting an exception to a response, using a message.
     */
    @Test
    public void testToResponse() {
        JerseyExceptionMapper mapper = new JerseyExceptionMapper();
        WebApplicationException e = new WebApplicationException("test");

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                r.getStatus());
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, er.getHttpStatus());
        Assert.assertEquals("test", er.getErrorDescription());
        Assert.assertNull(er.getRedirectUrl());
    }

    /**
     * Test mapping Not Found.
     */
    @Test
    public void testNotFound() {
        JerseyExceptionMapper mapper = new JerseyExceptionMapper();
        NotFoundException e = new NotFoundException();

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
        Assert.assertEquals(Status.NOT_FOUND, er.getHttpStatus());
        Assert.assertEquals("HTTP 404 Not Found", er.getErrorDescription());
        Assert.assertNull(er.getRedirectUrl());
        Assert.assertEquals("not_found", er.getError());
    }

    /**
     * Test mapping a forbidden error.
     */
    @Test
    public void testForbidden() {
        JerseyExceptionMapper mapper = new JerseyExceptionMapper();
        ForbiddenException e = new ForbiddenException();

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), r.getStatus());
        Assert.assertEquals(Status.FORBIDDEN, er.getHttpStatus());
        Assert.assertEquals("HTTP 403 Forbidden", er.getErrorDescription());
        Assert.assertNull(er.getRedirectUrl());
        Assert.assertEquals("forbidden", er.getError());
    }
}
