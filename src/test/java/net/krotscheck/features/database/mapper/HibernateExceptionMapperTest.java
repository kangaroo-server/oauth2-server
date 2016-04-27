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

package net.krotscheck.features.database.mapper;

import net.krotscheck.features.exception.ErrorResponseBuilder.ErrorResponse;
import org.hibernate.HibernateException;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;

/**
 * Unit tests for the hibernate exception mapper. exception mapper.
 *
 * @author Michael Krotscheck
 */
public final class HibernateExceptionMapperTest {

    /**
     * Assert that an usual search exceptions map to a 500.
     */
    @Test
    public void testToResponse() {
        HibernateExceptionMapper mapper = new HibernateExceptionMapper();
        HibernateException e = new HibernateException("test");

        Response r = mapper.toResponse(e);
        ErrorResponse er = (ErrorResponse) r.getEntity();

        Assert.assertEquals(500, r.getStatus());
        Assert.assertEquals(500, er.getHttpStatus());
        Assert.assertEquals("Internal Server Error", er.getErrorMessage());
        Assert.assertEquals("", er.getRedirectUrl());
    }
}
