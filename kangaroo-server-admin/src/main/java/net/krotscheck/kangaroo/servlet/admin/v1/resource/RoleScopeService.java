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

import net.krotscheck.kangaroo.common.exception.exception.HttpStatusException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidScopeException;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.Role;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.servlet.admin.v1.filter.OAuth2;
import org.apache.http.HttpStatus;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScope;
import org.hibernate.Session;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.UUID;

/**
 * A RESTful API that permits the management of application role resources.
 *
 * @author Michael Krotscheck
 */
@OAuth2
@RolesAllowed({Scope.ROLE, Scope.ROLE_ADMIN})
@Transactional
public final class RoleScopeService extends AbstractService {

    /**
     * The role from which the scopes are extracted.
     */
    private UUID roleId;

    /**
     * Set the role for this instance of the scope service.
     *
     * @param roleId The role id.
     */
    public void setRoleId(final UUID roleId) {
        this.roleId = roleId;
    }

    /**
     * Create a link between a role and a scope.
     *
     * @param scopeId The ID of the scope to link.
     * @return A redirect to the location where the role was created.
     */
    @POST
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    public Response createResource(@PathParam("id") final UUID scopeId) {
        Session s = getSession();
        SecurityContext security = getSecurityContext();

        // Make sure we're allowed to access the role.
        Role role = s.get(Role.class, roleId);
        assertCanAccess(role, getAdminScope());

        // Make sure we're allowed to access the scope. Since the scope check
        // for the second required scope is not handled for us, we have to do
        // it here.
        ApplicationScope scope = s.get(ApplicationScope.class, scopeId);
        if (!security.isUserInRole(Scope.SCOPE)
                && !security.isUserInRole(Scope.SCOPE_ADMIN)) {
            throw new InvalidScopeException();
        }
        assertCanAccess(scope, Scope.SCOPE_ADMIN);

        // If the parent application doesn't match, error.
        if (!role.getApplication().equals(scope.getApplication())) {
            throw new HttpStatusException(HttpStatus.SC_BAD_REQUEST);
        }

        // If the role is already linked to this scope, error.
        if (role.getScopes().values().contains(scope)) {
            throw new HttpStatusException(HttpStatus.SC_CONFLICT);
        }

        // If we're trying to modify the admin application, error.
        if (role.getApplication().equals(getAdminApplication())) {
            throw new HttpStatusException(HttpStatus.SC_FORBIDDEN);
        }

        // Create the link.
        role.getScopes().put(scope.getName(), scope);
        s.update(role);

        return Response.created(getUriInfo().getAbsolutePath()).build();
    }


    /**
     * Delete a scope from a role.
     *
     * @param scopeId The Unique Identifier for the scope.
     * @return A response that indicates the success of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}}")
    public Response deleteResource(@PathParam("id") final UUID scopeId) {
        Session s = getSession();
        SecurityContext security = getSecurityContext();

        // Make sure we're allowed to access the role.
        Role role = s.get(Role.class, roleId);
        assertCanAccess(role, getAdminScope());

        // Make sure we're allowed to access the scope. Since the scope check
        // for the second required scope is not handled for us, we have to do
        // it here.
        ApplicationScope scope = s.get(ApplicationScope.class, scopeId);
        if (!security.isUserInRole(Scope.SCOPE)
                && !security.isUserInRole(Scope.SCOPE_ADMIN)) {
            throw new InvalidScopeException();
        }
        assertCanAccess(scope, Scope.SCOPE_ADMIN);

        // If the scope's not assigned to the role, error.
        if (!role.getScopes().values().contains(scope)) {
            throw new HttpStatusException(HttpStatus.SC_NOT_FOUND);
        }

        // If we're in the admin app, we can't modify anything.
        if (getAdminApplication().equals(role.getApplication())) {
            throw new HttpStatusException(HttpStatus.SC_FORBIDDEN);
        }

        // Execute the command.
        role.getScopes().remove(scope.getName());
        s.update(role);

        return Response.noContent().build();
    }

    /**
     * Return the scope required to access ALL resources on this services.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAdminScope() {
        return Scope.ROLE_ADMIN;
    }

    /**
     * Return the scope required to access resources on this service.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAccessScope() {
        return Scope.ROLE;
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(RoleScopeService.class)
                    .to(RoleScopeService.class)
                    .to(RequestScope.class);
        }
    }
}
