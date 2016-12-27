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
 *
 */

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.Role;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import net.krotscheck.kangaroo.test.HttpUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * Test the CRUD methods of the scope service.
 *
 * @author Michael Krotscheck
 */
public final class RoleServiceCRUDTest
        extends AbstractServiceCRUDTest<Role> {

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType    The type of  client.
     * @param tokenScope    The client scope to issue.
     * @param createUser    Whether to create a new user.
     * @param shouldSucceed Should this test succeed?
     */
    public RoleServiceCRUDTest(final ClientType clientType,
                               final String tokenScope,
                               final Boolean createUser,
                               final Boolean shouldSucceed) {
        super(Role.class, clientType, tokenScope, createUser, shouldSucceed);
    }

    /**
     * Test parameters.
     *
     * @return List of parameters used to reconstruct this test.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.ROLE_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.ROLE,
                        false,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.ROLE_ADMIN,
                        true,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.ROLE,
                        true,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.ROLE_ADMIN,
                        false,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.ROLE,
                        false,
                        false
                });
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected String getUrlForId(final String id) {
        if (id == null) {
            return "/role/";
        }
        return String.format("/role/%s", id);
    }

    /**
     * Construct the request URL for a subresource ID.
     *
     * @param id    The root ID to use.
     * @param subId The subresource ID to use.
     * @return The resource URL.
     */
    private String getUrlForSubresourceId(final String id,
                                          final String subId) {
        String firstPath = getUrlForId(id);

        if (subId == null) {
            return String.format("%s/scope/", firstPath);
        }
        return String.format("%s/scope/%s", firstPath, subId);
    }

    /**
     * Extract the appropriate entity from a provided context.
     *
     * @return The client currently active in the admin app.
     */
    @Override
    protected Role getEntity(final EnvironmentBuilder context) {
        return context.getRole();
    }

    /**
     * Create a brand new entity.
     *
     * @return A brand new entity!
     */
    @Override
    protected Role getNewEntity() {
        return new Role();
    }

    /**
     * Return the token scope required for admin access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.ROLE_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.ROLE;
    }

    /**
     * Create a new valid entity to test the creation endpoint.
     *
     * @param context The context within which to create the entity.
     * @return A valid, but unsaved, entity.
     */
    @Override
    protected Role createValidEntity(final EnvironmentBuilder context) {
        Role role = new Role();
        role.setApplication(context.getApplication());
        role.setName(RandomStringUtils.randomAlphanumeric(10));
        return role;
    }

    /**
     * Assert that you cannot create a role without an application
     * reference.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoParent() throws Exception {
        Role testEntity = createValidEntity(getAdminContext());
        testEntity.setApplication(null);

        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test a really long name.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostTooLongName() throws Exception {
        Role testEntity = createValidEntity(getAdminContext());
        testEntity.setName(RandomStringUtils.randomAlphanumeric(257));

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test a really short name.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostTooShortName() throws Exception {
        Role testEntity = createValidEntity(getAdminContext());
        testEntity.setName(RandomStringUtils.randomAlphanumeric(2));

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test no name.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPostNoName() throws Exception {
        Role testEntity = createValidEntity(getAdminContext());
        testEntity.setName(null);

        // Issue the request.
        Response r = postEntity(testEntity, getAdminToken());
        assertErrorResponse(r, Status.BAD_REQUEST);
    }

    /**
     * Test that a role may be updated.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutRole() throws Exception {
        Role testEntity = getEntity(getSecondaryContext());
        testEntity.setName("new name");

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());

        if (shouldSucceed()) {
            Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
            Role result = r.readEntity(Role.class);
            Assert.assertEquals(testEntity.getId(), result.getId());
            Assert.assertEquals("new name", testEntity.getName());
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Test that a role cannot have its application updated.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutChangeApplication() throws Exception {
        Application otherApplication = getAdminContext()
                .getApplication();
        Role testEntity = getEntity(getSecondaryContext());

        testEntity.setApplication(otherApplication);

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Test that a role in the admin app cannot be changed.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testPutAdminApp() throws Exception {
        Role testEntity = getEntity(getAdminContext());
        testEntity.setName("new name");

        // Issue the request.
        Response r = putEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.FORBIDDEN);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }


    /**
     * Assert that we cannot delete an admin role.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testDeleteAdminRole() throws Exception {
        Role testEntity = getEntity(getAdminContext());

        // Issue the request.
        Response r = deleteEntity(testEntity, getAdminToken());
        if (shouldSucceed()) {
            assertErrorResponse(r, Status.FORBIDDEN);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we can link a scope.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testAddScope() throws Exception {
        Session s = getSession();

        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for an existing role and a new scope.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = getEntity(context);
        ApplicationScope scope = context.scope("test").getScope();
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .post(null);

        if (shouldSucceed()) {
            Assert.assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

            s.refresh(role);
            Assert.assertTrue(role.getScopes().values().contains(scope));

            // Cleanup
            Transaction t = s.beginTransaction();
            role.getScopes().remove(scope.getName());
            s.update(role);
            t.commit();
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot add already linked scopes.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testAddAlreadyLinkedScope() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for an existing role and a new scope.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = getEntity(context);
        ApplicationScope scope = context.getScope();
        Assert.assertTrue(role.getScopes().values().contains(scope));
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .post(null);

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.CONFLICT);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot link an invalid application scope.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testAddInvalidAppScope() throws Exception {
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for an existing role and a new scope.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = getEntity(context);
        String url = getUrlForSubresourceId(role.getId().toString(),
                UUID.randomUUID().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .post(null);

        if (shouldSucceed()) {
            // Not found because it doesn't exist.
            assertErrorResponse(r, Status.NOT_FOUND);
        } else {
            // Not found because we can't tell you about it.
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot link a malformed scope.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testAddMalformedScope() throws Exception {
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for an existing role and a new scope.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = getEntity(context);
        String url = getUrlForSubresourceId(role.getId().toString(),
                "malformed");

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .post(null);

        if (shouldSucceed()) {
            // Not found because we can't map a path.
            assertErrorResponse(r, Status.NOT_FOUND);
        } else {
            // Not found because we can't tell you about it.
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a scope and a role from different parent applications
     * cannot be linked.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testAddScopeApplicationMismatch() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for an existing role and a new scope.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = getEntity(context);
        ApplicationScope scope = getAdminContext().scope("test").getScope();
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .post(null);

        if (shouldSucceed()) {
            // Bad request because OMG really?
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            // Bad request because we can't tell you about it.
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a token without an applicationscope scope fails.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testAddScopeNoSubresourcePermission() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope()).getToken();

        // Build our request URI for an existing role and a new scope.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = getEntity(context);
        ApplicationScope scope = context.scope("test").getScope();
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .post(null);

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST, "invalid_scope");
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that a token with Scope.SCOPE may work.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testAddScopeRegularSubresourcePermission() throws Exception {
        Session s = getSession();
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE).getToken();

        // Build our request URI for an existing role and a new scope.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = getEntity(context);
        ApplicationScope scope = context.scope("test").getScope();
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        Boolean shouldSucceed = shouldSucceed()
                && isAccessible(scope, token, Scope.SCOPE_ADMIN);

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .post(null);

        if (shouldSucceed) {
            Assert.assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

            s.refresh(role);
            Assert.assertTrue(role.getScopes().values().contains(scope));

            // Cleanup
            Transaction t = s.beginTransaction();
            role.getScopes().remove(scope.getName());
            s.update(role);
            t.commit();
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot modify the admin application.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testAddAdminApplication() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for an existing role and a new scope.
        EnvironmentBuilder context = getAdminContext();
        Role role = getEntity(context);
        ApplicationScope scope = context.scope("test").getScope();
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .post(null);

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.FORBIDDEN);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we can remove a scope.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testRemoveScope() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for existing linked roles and scopes.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = context.getRole();
        ApplicationScope scope = context.getScope();
        Assert.assertTrue(role.getScopes().values().contains(scope));
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .delete();

        if (shouldSucceed()) {
            Assert.assertEquals(Status.NO_CONTENT.getStatusCode(),
                    r.getStatus());

            getSession().refresh(role);
            Assert.assertFalse(role.getScopes().values().contains(scope));
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot remove an invalid scope.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testRemoveInvalidScope() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for existing linked roles and scopes.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = context.getRole();
        String url = getUrlForSubresourceId(role.getId().toString(),
                UUID.randomUUID().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .delete();

        if (shouldSucceed()) {
            // Not found because it doesn't exist.
            assertErrorResponse(r, Status.NOT_FOUND);
        } else {
            // Not found because we can't tell you about it.
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot remove a valid scope that hasn't been linked.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testRemoveUnlinkedScope() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        String scopes = String.format("%s %s", getTokenScope(),
                Scope.SCOPE_ADMIN);
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for existing linked roles and scopes.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = context.getRole();
        ApplicationScope scope = context.scope("test").getScope();
        Assert.assertFalse(role.getScopes().values().contains(scope));
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .delete();

        if (shouldSucceed()) {
            // Not found because it's not linked.
            assertErrorResponse(r, Status.NOT_FOUND);
        } else {
            // Not found because we can't tell you about it.
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot remove a malformed scope.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testRemoveMalformedScope() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for existing linked roles and scopes.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = context.getRole();
        String url = getUrlForSubresourceId(role.getId().toString(),
                "malformed");

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .delete();

        if (shouldSucceed()) {
            // Not found because it doesn't exist.
            assertErrorResponse(r, Status.NOT_FOUND);
        } else {
            // Not found because we can't tell you about it.
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that deleting using a token without an applicationscope scope
     * fails.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testRemoveScopeNoSubresourcePermission() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope()).getToken();

        // Build our request URI for existing linked roles and scopes.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = context.getRole();
        ApplicationScope scope = context.getScope();
        Assert.assertTrue(role.getScopes().values().contains(scope));
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .delete();

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.BAD_REQUEST, "invalid_scope");
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that deleting using a token with Scope.SCOPE may work.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testRemoveScopeRegularSubresourcePermission() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                        getTokenScope(),
                        Scope.SCOPE).getToken();

        // Build our request URI for existing linked roles and scopes.
        EnvironmentBuilder context = getSecondaryContext();
        Role role = context.getRole();
        ApplicationScope scope = context.getScope();
        Assert.assertTrue(role.getScopes().values().contains(scope));
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        Boolean shouldSucceed = shouldSucceed()
                && isAccessible(scope, token, Scope.SCOPE_ADMIN);

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .delete();

        if (shouldSucceed) {
            Assert.assertEquals(Status.NO_CONTENT.getStatusCode(),
                    r.getStatus());

            getSession().refresh(role);
            Assert.assertFalse(role.getScopes().values().contains(scope));
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Assert that we cannot modify the admin application.
     *
     * @throws Exception Exception encountered during test.
     */
    @Test
    public void testRemoveScopeAdminApplication() throws Exception {
        // We're using an admin auth token here, but we're modifying an app
        // other than the admin app.
        OAuthToken token = getAdminContext()
                .bearerToken(getAdminClient(),
                getTokenScope(),
                Scope.SCOPE_ADMIN).getToken();

        // Build our request URI for existing linked roles and scopes.
        EnvironmentBuilder context = getAdminContext();
        Role role = context.getRole();
        ApplicationScope scope = role.getScopes().values().iterator().next();
        Assert.assertTrue(role.getScopes().values().contains(scope));
        String url = getUrlForSubresourceId(role.getId().toString(),
                scope.getId().toString());

        // Execute the request.
        Response r = target(url)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getId()))
                .delete();

        if (shouldSucceed()) {
            assertErrorResponse(r, Status.FORBIDDEN);
        } else {
            assertErrorResponse(r, Status.NOT_FOUND);
        }
    }

    /**
     * Test that our scope getters on the services return the appropriate
     * values.
     */
    @Test
    public void testGetScopes() {
        RoleService roleService = new RoleService();
        Assert.assertEquals(Scope.ROLE, roleService.getAccessScope());
        Assert.assertEquals(Scope.ROLE_ADMIN, roleService.getAdminScope());

        RoleScopeService roleScopeService = new RoleScopeService();
        Assert.assertEquals(Scope.ROLE, roleScopeService.getAccessScope());
        Assert.assertEquals(Scope.ROLE_ADMIN, roleScopeService.getAdminScope());
    }
}