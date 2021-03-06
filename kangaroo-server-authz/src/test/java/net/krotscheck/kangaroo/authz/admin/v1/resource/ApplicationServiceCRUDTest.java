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

package net.krotscheck.kangaroo.authz.admin.v1.resource;

import net.krotscheck.kangaroo.authz.admin.Scope;
import net.krotscheck.kangaroo.authz.admin.v1.exception.InvalidEntityPropertyException;
import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.Role;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.response.ListResponseEntity;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * Unit tests for the /application endpoint's CRUD methods.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class ApplicationServiceCRUDTest
        extends AbstractServiceCRUDTest<Application> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<ListResponseEntity<Application>>
            LIST_TYPE = new GenericType<ListResponseEntity<Application>>() {

    };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType    The type of  client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public ApplicationServiceCRUDTest(final ClientType clientType,
                                      final String tokenScope,
                                      final Boolean createUser,
                                      final Boolean shouldSucceed) {
        super(Application.class, clientType, tokenScope, createUser,
                shouldSucceed);
    }

    /**
     * Test parameters.
     *
     * @return A list of parameters used to initialize the test class.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION_ADMIN,
                        true,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.APPLICATION,
                        true,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.APPLICATION_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.APPLICATION,
                        false,
                        false
                });
    }

    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<ListResponseEntity<Application>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Return the correct testingEntity type from the provided context.
     *
     * @param context The context to extract the value from.
     * @return The requested entity type under test.
     */
    @Override
    protected Application getEntity(final ApplicationContext context) {
        return context.getApplication();
    }

    /**
     * Return a new, empty entity.
     *
     * @return The requested entity type under test.
     */
    @Override
    protected Application getNewEntity() {
        return new Application();
    }

    /**
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    @Override
    protected Application createValidEntity(final ApplicationContext context) {
        Application a = new Application();
        a.setName(IdUtil.toString(IdUtil.next()));
        a.setOwner(context.getOwner());
        return a;
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.APPLICATION_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.APPLICATION;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        UriBuilder builder = UriBuilder.fromPath("/v1/application/");
        if (id != null) {
            builder.path(id);
        }
        return builder.build();
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param entity The entity to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForEntity(final AbstractAuthzEntity entity) {
        if (entity == null || entity.getId() == null) {
            return getUrlForId(null);
        }
        return getUrlForId(IdUtil.toString(entity.getId()));
    }

    /**
     * Assert that an app cannot be created which overwrites another app.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostOverwrite() throws Exception {
        ApplicationContext context = getAdminContext();

        Application newApp = new Application();
        newApp.setId(getSecondaryContext().getApplication().getId());
        newApp.setName("New Application");
        newApp.setOwner(context.getUser());

        // Issue the request.
        Response r = postEntity(newApp, getAdminToken());

        assertErrorResponse(r,
                new InvalidEntityPropertyException("id"));
    }

    /**
     * Test a really long name.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostTooLongName() throws Exception {
        ApplicationContext context = getAdminContext();

        Application newApp = new Application();
        newApp.setName(RandomStringUtils.randomAlphanumeric(257));
        newApp.setOwner(context.getUser());

        // Issue the request.
        Response r = postEntity(newApp, getAdminToken());
        if (this.isAccessible(newApp, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST,
                    "bad_request",
                    "Application name must be between 3 and 255 characters.");
        } else {
            assertErrorResponse(r, new BadRequestException());
        }
    }

    /**
     * Test a really long description.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostTooLongDescription() throws Exception {
        ApplicationContext context = getAdminContext();

        Application newApp = new Application();
        newApp.setName(RandomStringUtils.randomAlphanumeric(32));
        newApp.setDescription(RandomStringUtils.randomAlphanumeric(257));
        newApp.setOwner(context.getUser());

        // Issue the request.
        Response r = postEntity(newApp, getAdminToken());
        if (this.isAccessible(newApp, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST,
                    "bad_request",
                    "Application description cannot exceed 255 characters.");
        } else {
            assertErrorResponse(r, new BadRequestException());
        }
    }

    /**
     * Test creating an application with a default role from another
     * application.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostDefaultRoleFromOtherApp() throws Exception {
        ApplicationContext context = getAdminContext();

        Application newApp = new Application();
        newApp.setName(RandomStringUtils.randomAlphanumeric(257));
        newApp.setOwner(context.getUser());
        newApp.setDefaultRole(context.getRole());

        // Issue the request.
        Response r = postEntity(newApp, getAdminToken());
        if (this.isAccessible(newApp, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST,
                    "bad_request",
                    "Application name must be between 3 and 255 characters.");
        } else {
            assertErrorResponse(r, new BadRequestException());
        }
    }

    /**
     * Test creating an application a default role that does not exist.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNonexistentDefaultRole() throws Exception {
        ApplicationContext context = getAdminContext();

        Role defaultRole = new Role();
        defaultRole.setId(IdUtil.next());

        Application newApp = new Application();
        newApp.setName(RandomStringUtils.randomAlphanumeric(257));
        newApp.setOwner(context.getUser());
        newApp.setDefaultRole(defaultRole);

        // Issue the request.
        Response r = postEntity(newApp, getAdminToken());
        if (this.isAccessible(newApp, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST,
                    "bad_request",
                    "Application name must be between 3 and 255 characters.");
        } else {
            assertErrorResponse(r, new BadRequestException());
        }
    }

    /**
     * Test that an app created with no owner defaults to the current user.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoOwner() throws Exception {
        OAuthToken token = getAdminToken();

        Application newApp = new Application();
        newApp.setName("New Application");

        // Issue the request.
        Response r = postEntity(newApp, token);

        if (!token.getClient().getType().equals(ClientType.ClientCredentials)) {
            assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
            assertNotNull(r.getLocation());

            Response getResponse =
                    getEntity(r.getLocation(), getAdminToken());
            Application response =
                    getResponse.readEntity(Application.class);
            assertNotNull(response.getId());
            assertEquals(newApp.getName(), response.getName());
            assertNotNull(response.getOwner().getId());
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Test that an app can be created for another user.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostOwnerAssign() throws Exception {
        ApplicationContext testContext = getAdminContext()
                .getBuilder()
                .user()
                .identity()
                .build();
        OAuthToken token = getAdminToken();

        Application newApp = new Application();
        newApp.setName("New Application");
        newApp.setOwner(testContext.getUser());

        // Issue the request.
        Response r = postEntity(newApp, token);

        if (getTokenScope().equals(Scope.APPLICATION_ADMIN)) {
            assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
            assertNotNull(r.getLocation());

            Response getResponse = getEntity(r.getLocation(), getAdminToken());
            Application response = getResponse.readEntity(Application.class);

            assertNotNull(response.getId());
            assertEquals(newApp.getName(), response.getName());
            assertEquals(newApp.getOwner(), response.getOwner());
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Assert that the admin app cannot be updated.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutAdminApp() throws Exception {
        Application app = getAdminContext().getApplication();
        app.setName(IdUtil.toString(IdUtil.next()));

        Response r = putEntity(app, getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.FORBIDDEN);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a regular app can be updated, from the admin app, with
     * appropriate credentials.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRegularApp() throws Exception {
        Application a = getSecondaryContext().getApplication();
        a.setName(IdUtil.toString(IdUtil.next()));
        Response r = putEntity(a, getAdminToken());

        if (isAccessible(a, getAdminToken())) {
            Application response = r.readEntity(Application.class);
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertEquals(a, response);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that an application with no default role can have one set.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutSetDefaultRole() throws Exception {
        // Create a new application for the secondary user.
        Application newApp = createValidEntity(getAdminContext());

        // Create an unaffiliated new role.
        Role newRole = new Role();
        newRole.setApplication(newApp);
        newRole.setName("name");

        Session s = getSession();
        Transaction t = s.beginTransaction();
        s.save(newApp);
        s.save(newRole);
        t.commit();

        assertNull(newApp.getDefaultRole());

        // Try to set the default role.
        newApp.setDefaultRole(newRole);

        // Issue the request.
        Response r = putEntity(newApp, getAdminToken());

        if (isAccessible(newApp, getAdminToken())) {
            Application response = r.readEntity(Application.class);
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertEquals(newApp, response);
            assertEquals(newRole.getId(),
                    response.getDefaultRole().getId());
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }

    }

    /**
     * Assert that an application with a default role can have it updated
     * with another role.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutUpdateDefaultRole() throws Exception {
        Application newApp = createValidEntity(getAdminContext());

        Role originalRole = new Role();
        originalRole.setApplication(newApp);
        originalRole.setName("original");

        Role newRole = new Role();
        newRole.setApplication(newApp);
        newRole.setName("new");

        newApp.setDefaultRole(originalRole);

        Session s = getSession();
        Transaction t = s.beginTransaction();
        s.save(newApp);
        s.save(originalRole);
        s.save(newRole);
        t.commit();

        Application testEntity = new Application();
        testEntity.setDefaultRole(newRole);
        testEntity.setName(newApp.getName());
        testEntity.setId(newApp.getId());
        testEntity.setOwner(newApp.getOwner());

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());

        if (isAccessible(testEntity, getAdminToken())) {
            Application response = r.readEntity(Application.class);
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            assertEquals(testEntity, response);
            assertEquals(newRole.getId(),
                    response.getDefaultRole().getId());
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that an application cannot have its existing default role cleared.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutEmptyDefaultRole() throws Exception {
        Application a = getSecondaryContext().getApplication();
        assertNotNull(a.getDefaultRole());

        Application testEntity = new Application();
        testEntity.setDefaultRole(null);
        testEntity.setId(a.getId());
        testEntity.setName(a.getName());
        testEntity.setOwner(a.getOwner());

        Response r = putEntity(testEntity, getAdminToken());

        if (isAccessible(a, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert you cannot assign a nonexistent role as a default role.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutInvalidDefaultRole() throws Exception {
        Application a = getSecondaryContext().getApplication();
        Role role = new Role();
        role.setId(IdUtil.next());

        Application testEntity = new Application();
        testEntity.setDefaultRole(role);
        testEntity.setId(a.getId());
        testEntity.setName(a.getName());
        testEntity.setOwner(a.getOwner());

        Response r = putEntity(testEntity, getAdminToken());

        if (isAccessible(a, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that you cannot assign a default role to a different application.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutOtherAppDefaultRole() throws Exception {
        Application a = getSecondaryContext().getApplication();

        Application testEntity = new Application();
        testEntity.setDefaultRole(getAdminContext().getRole());
        testEntity.setId(a.getId());
        testEntity.setName(a.getName());
        testEntity.setOwner(a.getOwner());

        Response r = putEntity(testEntity, getAdminToken());

        if (isAccessible(a, getAdminToken())) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a regular app cannot have its owner changed.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutChangeOwner() throws Exception {
        // Create a new application for the secondary user.
        User oldOwner = getAdminContext().getOwner();
        Application newApp = createValidEntity(getAdminContext());
        newApp.setOwner(oldOwner);
        Session s = getSession();
        s.getTransaction().begin();
        s.save(newApp);
        s.getTransaction().commit();

        // Try to change the ownership to the admin user.
        User newOwner = getSecondaryContext().getOwner();
        newApp.setOwner(newOwner);

        // Issue the request.
        Response r = putEntity(newApp, getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r,
                    new InvalidEntityPropertyException("owner"));
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that the admin app cannot be deleted, even if we have all the
     * credentials in the world.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testDeleteAdminApp() throws Exception {
        ApplicationContext context = getAdminContext();

        // Issue the request.
        Response r = deleteEntity(context.getApplication(), getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.FORBIDDEN);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Sanity test for coverage on the scope getters.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testScopes() throws Exception {
        ApplicationService as = new ApplicationService();

        assertEquals(Scope.APPLICATION_ADMIN, as.getAdminScope());
        assertEquals(Scope.APPLICATION, as.getAccessScope());
    }

    /**
     * Assert that the admin app cannot be deleted, even if we have all the
     * credentials in the world.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void test() throws Exception {
        ApplicationContext context = getAdminContext();

        // Issue the request.
        Response r = deleteEntity(context.getApplication(), getAdminToken());

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.FORBIDDEN);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }
}
