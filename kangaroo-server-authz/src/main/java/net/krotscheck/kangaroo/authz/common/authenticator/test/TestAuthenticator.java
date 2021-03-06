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

package net.krotscheck.kangaroo.authz.common.authenticator.test;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.exception.MisconfiguredAuthenticatorException;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.Role;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.common.exception.KangarooException;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;

/**
 * The test authenticator provides a simple authenticator implementation which
 * may be used when building a new application and debugging the
 * authentication flow. It only ever creates a single user, "Pat Developer",
 * and always presumes a successful third-party authentication.
 *
 * @author Michael Krotscheck
 */
public final class TestAuthenticator
        implements IAuthenticator {

    /**
     * Unique foreign ID string for the debug user.
     */
    public static final String REMOTE_ID = "dev_user";

    /**
     * Hibernate session, to use for database access.
     */
    private final Session session;

    /**
     * Create a new dev authenticator.
     *
     * @param session Injected hibernate session.
     */
    @Inject
    public TestAuthenticator(final Session session) {
        this.session = session;
    }

    /**
     * Execute an authentication process for a specific request.
     *
     * @param configuration The authenticator configuration.
     * @param callback      The redirect, on this server, where the response
     *                      should go.
     * @return An HTTP response, redirecting the client to the next step.
     */
    @Override
    public Response delegate(final Authenticator configuration,
                             final URI callback) {
        return Response
                .status(Status.FOUND)
                .location(callback)
                .build();
    }

    /**
     * Resolve and/or create a user identity, given an intermediate state and
     * request parameters.
     *
     * @param authenticator The authenticator configuration.
     * @param parameters    Parameters for the authenticator, retrieved from
     *                      an appropriate source.
     * @param callback      The redirect that was provided to the original
     *                      authorize call.
     */
    @Override
    public UserIdentity authenticate(final Authenticator authenticator,
                                     final MultivaluedMap<String, String>
                                             parameters,
                                     final URI callback) {
        Criteria searchCriteria = session.createCriteria(UserIdentity.class);

        searchCriteria.add(Restrictions.eq("type", authenticator.getType()));
        searchCriteria.add(Restrictions.eq("remoteId", REMOTE_ID));

        searchCriteria.createAlias("user", "u");
        searchCriteria.add(Restrictions.eq("u.application",
                authenticator.getClient().getApplication()));

        searchCriteria.setFirstResult(0);
        searchCriteria.setMaxResults(1);

        UserIdentity identity = (UserIdentity) searchCriteria.uniqueResult();

        // Do we need to create a new user?
        if (identity == null) {
            Role testRole = authenticator.getClient().getApplication()
                    .getDefaultRole();

            User devUser = new User();
            devUser.setApplication(authenticator.getClient().getApplication());
            devUser.setRole(testRole);

            identity = new UserIdentity();
            identity.setType(authenticator.getType());
            identity.setRemoteId(REMOTE_ID);
            identity.setUser(devUser);

            session.save(devUser);
            session.save(identity);
        }

        return identity;
    }

    /**
     * Validate the test authenticator.
     *
     * @param authenticator The authenticator configuration.
     * @throws KangarooException Thrown if the "invalid" property is sent.
     */
    @Override
    public void validate(final Authenticator authenticator)
            throws KangarooException {
        // The test authenticator will fail if a "invalid" property is set.
        if (authenticator.getConfiguration().containsKey("invalid")) {
            throw new MisconfiguredAuthenticatorException();
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(TestAuthenticator.class)
                    .to(IAuthenticator.class)
                    .named(AuthenticatorType.Test.name())
                    .in(RequestScoped.class);
        }
    }
}
