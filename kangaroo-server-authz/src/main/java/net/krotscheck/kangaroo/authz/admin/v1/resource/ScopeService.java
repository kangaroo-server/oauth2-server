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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import net.krotscheck.kangaroo.authz.admin.Scope;
import net.krotscheck.kangaroo.authz.admin.v1.auth.ScopesAllowed;
import net.krotscheck.kangaroo.authz.admin.v1.exception.EntityRequiredException;
import net.krotscheck.kangaroo.authz.admin.v1.exception.InvalidEntityPropertyException;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Role;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.util.SortUtil;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.hibernate.transaction.Transactional;
import net.krotscheck.kangaroo.common.response.ApiParam;
import net.krotscheck.kangaroo.common.response.ListResponseBuilder;
import net.krotscheck.kangaroo.common.response.SortOrder;
import org.apache.lucene.search.Query;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.jvnet.hk2.annotations.Optional;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.math.BigInteger;
import java.net.URI;


/**
 * A RESTful API that permits the management of scope resources.
 *
 * @author Michael Krotscheck
 */
@Path("/v1/scope")
@ScopesAllowed({Scope.SCOPE, Scope.SCOPE_ADMIN})
@Transactional
@Api(tags = "Scope",
        authorizations = {
                @Authorization(value = "Kangaroo", scopes = {
                        @AuthorizationScope(
                                scope = Scope.SCOPE,
                                description = "Modify scopes in one"
                                        + " application."),
                        @AuthorizationScope(
                                scope = Scope.SCOPE_ADMIN,
                                description = "Modify scopes in all"
                                        + " applications.")
                })
        })
public final class ScopeService extends AbstractService {

    /**
     * Search the scopes in the system.
     *
     * @param offset        The offset of the first scopes to fetch.
     * @param limit         The number of data sets to fetch.
     * @param queryString   The search term for the query.
     * @param ownerId       An optional user ID to filter by.
     * @param applicationId An optional application ID to filter by.
     * @param roleId        An optional role ID to filter by.
     * @return A list of search results.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Search scopes")
    @SuppressWarnings({"CPD-START"})
    public Response search(
            @DefaultValue("0") @QueryParam("offset") final Integer offset,
            @DefaultValue("10") @QueryParam("limit") final Integer limit,
            @DefaultValue("") @QueryParam("q") final String queryString,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("owner") final BigInteger ownerId,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("application") final BigInteger applicationId,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("role") final BigInteger roleId) {

        // Start a query builder...
        QueryBuilder builder = getSearchFactory()
                .buildQueryBuilder()
                .forEntity(ApplicationScope.class)
                .get();
        BooleanJunction junction = builder.bool();

        Query fuzzy = builder.keyword()
                .fuzzy()
                .onFields(new String[]{"name"})
                .matching(queryString)
                .createQuery();
        junction = junction.must(fuzzy);

        // Attach an ownership filter.
        User owner = resolveOwnershipFilter(ownerId);
        if (owner != null) {
            Query ownerQuery = builder
                    .keyword()
                    .onField("application.owner.id")
                    .matching(owner.getId())
                    .createQuery();
            junction.must(ownerQuery);
        }

        // Attach an application filter.
        Application filterByApp = resolveFilterEntity(Application.class,
                applicationId);
        if (filterByApp != null) {
            Query appQuery = builder
                    .keyword()
                    .onField("application.id")
                    .matching(filterByApp.getId())
                    .createQuery();
            junction.must(appQuery);
        }

        FullTextQuery query = getFullTextSession()
                .createFullTextQuery(junction.createQuery(),
                        ApplicationScope.class);

        return executeQuery(ApplicationScope.class, query, offset, limit);
    }

    /**
     * Browse the scopes in the system.
     *
     * @param offset        The offset of the first scopes to fetch.
     * @param limit         The number of data sets to fetch.
     * @param sort          The field on which the records should be sorted.
     * @param order         The sort order, ASC or DESC.
     * @param ownerId       An optional user ID to filter by.
     * @param applicationId An optional application ID to filter by.
     * @param roleId        An optional role ID to filter by.
     * @return A list of search results.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Browse scopes")
    public Response browseScopes(
            @QueryParam(ApiParam.OFFSET_QUERY)
            @DefaultValue(ApiParam.OFFSET_DEFAULT) final int offset,
            @QueryParam(ApiParam.LIMIT_QUERY)
            @DefaultValue(ApiParam.LIMIT_DEFAULT) final int limit,
            @QueryParam(ApiParam.SORT_QUERY)
            @DefaultValue(ApiParam.SORT_DEFAULT) final String sort,
            @QueryParam(ApiParam.ORDER_QUERY)
            @DefaultValue(ApiParam.ORDER_DEFAULT) final SortOrder order,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("owner") final BigInteger ownerId,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("application") final BigInteger applicationId,
            @io.swagger.annotations.ApiParam(type = "string")
            @Optional @QueryParam("role") final BigInteger roleId) {

        // Validate the incoming filters.
        User filterByOwner = resolveOwnershipFilter(ownerId);
        Application filterByApp = resolveFilterEntity(
                Application.class,
                applicationId);
        Role filterByRole = resolveFilterEntity(
                Role.class,
                roleId);

        // Assert that the sort is on a valid column
        Criteria countCriteria = getSession()
                .createCriteria(ApplicationScope.class)
                .createAlias("application", "a")
                .setProjection(Projections.rowCount());

        Criteria browseCriteria = getSession()
                .createCriteria(ApplicationScope.class)
                .createAlias("application", "a")
                .setFirstResult(offset)
                .setMaxResults(limit)
                .addOrder(SortUtil.order(order, sort));

        if (filterByApp != null) { // NOPMD - copy/paste
            browseCriteria.add(Restrictions.eq("a.id", filterByApp.getId()));
            countCriteria.add(Restrictions.eq("a.id", filterByApp.getId()));
        }

        if (filterByRole != null) { // NOPMD - copy/paste
            browseCriteria
                    .createAlias("roles", "r")
                    .add(Restrictions.eq("r.id", filterByRole.getId()));
            countCriteria
                    .createAlias("roles", "r")
                    .add(Restrictions.eq("r.id", filterByRole.getId()));
        }

        if (filterByOwner != null) { // NOPMD
            browseCriteria
                    .createAlias("a.owner", "o")
                    .add(Restrictions.eq("o.id", filterByOwner.getId()));
            countCriteria
                    .createAlias("a.owner", "o")
                    .add(Restrictions.eq("o.id", filterByOwner.getId()));
        }

        return ListResponseBuilder.builder()
                .offset(offset)
                .limit(limit)
                .order(order)
                .sort(sort)
                .total(countCriteria.uniqueResult())
                .addResult(browseCriteria.list())
                .build();
    }

    /**
     * Returns a specific scope.
     *
     * @param id The Unique Identifier for the scope.
     * @return A response with the scope that was requested.
     */
    @SuppressWarnings("CPD-END")
    @GET
    @Path("/{id: [a-f0-9]{32}}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Read scope")
    public Response getResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id) {
        ApplicationScope scope = getSession().get(ApplicationScope.class, id);
        assertCanAccess(scope, getAdminScope());
        return Response.ok(scope).build();
    }

    /**
     * Create an scope.
     *
     * @param scope The scope to create.
     * @return A response with the scope that was created.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create scope")
    public Response createResource(final ApplicationScope scope) {

        // Input value checks.
        if (scope == null) {
            throw new EntityRequiredException();
        }
        if (scope.getId() != null) {
            throw new InvalidEntityPropertyException("id");
        }
        if (scope.getApplication() == null) {
            throw new InvalidEntityPropertyException("application");
        }

        // Assert that we can create a scope in this application.
        if (!getSecurityContext().isUserInRole(getAdminScope())) {
            Application scopeApp =
                    getSession().get(Application.class,
                            scope.getApplication().getId());
            if (getCurrentUser() == null
                    || !getCurrentUser().equals(scopeApp.getOwner())) {
                throw new BadRequestException();
            }
        }

        // Save it all.
        Session s = getSession();
        s.save(scope);

        // Force a commit, to see what DB validation thinks of this.
        s.getTransaction().commit();

        // Build the URI of the new resources.
        URI resourceLocation = getUriInfo().getAbsolutePathBuilder()
                .path(ScopeService.class, "getResource")
                .build(IdUtil.toString(scope.getId()));

        return Response.created(resourceLocation).build();
    }

    /**
     * Update an scope.
     *
     * @param id    The Unique Identifier for the scope.
     * @param scope The scope to update.
     * @return A response with the scope that was updated.
     */
    @PUT
    @Path("/{id: [a-f0-9]{32}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update scope")
    public Response updateResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id,
            final ApplicationScope scope) {
        Session s = getSession();

        // Load the old instance.
        ApplicationScope currentScope = s.get(ApplicationScope.class, id);

        assertCanAccess(currentScope, getAdminScope());

        // Additional special case - we cannot modify the kangaroo app's scopes.
        if (currentScope.getApplication().equals(getAdminApplication())) {
            throw new ForbiddenException();
        }

        // Make sure the body ID's match
        if (!currentScope.equals(scope)) {
            throw new InvalidEntityPropertyException("id");
        }

        // Make sure we're not trying to change data we're not allowed.
        if (!currentScope.getApplication().equals(scope.getApplication())) {
            throw new InvalidEntityPropertyException("application");
        }

        // Transfer all the values we're allowed to edit.
        currentScope.setName(scope.getName());

        s.update(currentScope);

        return Response.ok(scope).build();
    }

    /**
     * Delete an scope.
     *
     * @param id The Unique Identifier for the scope.
     * @return A response that indicates the successs of this operation.
     */
    @DELETE
    @Path("/{id: [a-f0-9]{32}}")
    @ApiOperation(value = "Delete scope")
    public Response deleteResource(
            @io.swagger.annotations.ApiParam(type = "string")
            @PathParam("id") final BigInteger id) {
        Session s = getSession();
        ApplicationScope a = s.get(ApplicationScope.class, id);

        assertCanAccess(a, getAdminScope());

        // Additional special case - we cannot delete the kangaroo app itself.
        if (a.getApplication().equals(getAdminApplication())) {
            throw new ForbiddenException();
        }

        // Let's hope they now what they're doing.
        s.delete(a);

        return Response.status(Status.RESET_CONTENT).build();
    }

    /**
     * Return the scope required to access ALL resources on this services.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAdminScope() {
        return Scope.SCOPE_ADMIN;
    }

    /**
     * Return the scope required to access resources on this service.
     *
     * @return A string naming the scope.
     */
    @Override
    protected String getAccessScope() {
        return Scope.SCOPE;
    }
}
