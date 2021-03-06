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

package net.krotscheck.kangaroo.authz.oauth2.resource;

import net.krotscheck.kangaroo.authz.AuthzAPI;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientConfig;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.test.jersey.ContainerTest;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the /token endpoint. Note that this is not a
 * comprehensive suite, as it only covers edge cases and functionality not
 * otherwise covered in our RFC test suites.
 *
 * @author Michael Krotscheck
 */
public final class TokenServiceTest extends ContainerTest {

    /**
     * Simple testing context.
     */
    private static ApplicationContext context;

    /**
     * Test data loading for this test.
     */
    @ClassRule
    public static final TestRule TEST_DATA_RULE =
            new TestDataResource(HIBERNATE_RESOURCE) {
                /**
                 * Initialize the test data.
                 */
                @Override
                protected void loadTestData(final Session session) {
                    context = ApplicationBuilder.newApplication(session)
                            .client(ClientType.ClientCredentials, true)
                            .build();
                }
            };

    /**
     * Build and configure the application.
     *
     * @return A configured application.
     */
    @Override
    protected ResourceConfig createApplication() {
        return new AuthzAPI();
    }

    /**
     * Assert that an invalid token type is rejected.
     */
    @Test
    public void testInvalidGrantType() {
        Form testData = new Form();
        testData.param("grant_type", "invalid");
        testData.param("client_id",
                IdUtil.toString(context.getClient().getId()));
        testData.param("client_secret",
                context.getClient().getClientSecret());
        Entity testEntity = Entity.entity(testData,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        Response response = target("/oauth2/token")
                .request()
                .post(testEntity);

        ErrorResponse error = response.readEntity(ErrorResponse.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        assertEquals("invalid_grant", error.getError());
        assertNotNull(error.getErrorDescription());
    }

    /**
     * Assert that an valid token type is accepted.
     */
    @Test
    public void testValidGrantType() {
        // Using the client credentials type here, since it was the first one
        // created.
        Form testData = new Form();
        testData.param("grant_type", "client_credentials");
        testData.param("client_id",
                IdUtil.toString(context.getClient().getId()));
        testData.param("client_secret",
                context.getClient().getClientSecret());
        Entity testEntity = Entity.entity(testData,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        TokenResponseEntity token = target("/oauth2/token")
                .request()
                .post(testEntity, TokenResponseEntity.class);

        assertEquals(OAuthTokenType.Bearer, token.getTokenType());
        assertEquals((long) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                (long) token.getExpiresIn());
        assertNull(token.getRefreshToken());
        assertNull(token.getScope());
        assertNotNull(token.getAccessToken());
    }
}
