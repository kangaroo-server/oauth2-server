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

package net.krotscheck.kangaroo.authz.test;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.AuthenticatorState;
import net.krotscheck.kangaroo.authz.common.database.entity.Client;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientRedirect;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientReferrer;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.HttpSession;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.authz.common.database.entity.Role;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.common.util.PasswordUtil;
import net.krotscheck.kangaroo.common.hibernate.entity.AbstractEntity;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;

import javax.ws.rs.core.UriBuilder;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static net.krotscheck.kangaroo.authz.common.database.entity.ClientType.ClientCredentials;
import static net.krotscheck.kangaroo.authz.common.database.entity.OAuthTokenType.Refresh;

/**
 * This class assists in the creation of a test environment, by bootstrapping
 * applications, clients, their enabled authenticators and flows, as well as
 * other miscellaneous components.
 *
 * Note that this class is a bit volatile, as it makes the implicit
 * assumption that all the resources it needs will be created before they're
 * used. In other words, if you've got weird issues, then you're probably
 * using this class wrong.
 *
 * @author Michael Krotscheck
 */
public final class ApplicationBuilder {

    /**
     * Static timezone.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * The application context created by this builder.
     */
    private final ApplicationContext context;

    /**
     * The list of entities that are under management by this builder.
     */
    private final List<AbstractEntity> trackedEntities = new ArrayList<>();

    /**
     * Private default constructor.
     *
     * @param context The data context to wrap.
     */
    private ApplicationBuilder(final ApplicationContext context) {
        this.context = context;
    }

    /**
     * Private constructor.
     *
     * @param session     The session which this builder should use to persist
     *                    its data.`
     * @param application The root application instance.
     */
    private ApplicationBuilder(final Session session,
                               final Application application) {
        this.context = new ApplicationContext(session);
        this.context.application = application;
        this.persist(application);
    }

    /**
     * Create a new builder with a random name.
     *
     * @param session The session which this builder should use to persist
     *                its data.`
     * @return A new context builder.
     */
    public static ApplicationBuilder newApplication(final Session session) {
        return newApplication(session, IdUtil.toString(IdUtil.next()));
    }

    /**
     * Create a new builder.
     *
     * @param session The session which this builder should use to persist
     *                its data.`
     * @param name    The name of the application.
     * @return A new context builder.
     */
    public static ApplicationBuilder newApplication(final Session session,
                                                    final String name) {
        Application application = new Application();
        application.setName(name);
        return newApplication(session, application);
    }

    /**
     * Create a new builder.
     *
     * @param session     The session which this builder should use to persist
     *                    its data.`
     * @param application The new application.
     * @return A new context builder.
     */
    public static ApplicationBuilder newApplication(
            final Session session, final Application application) {
        return new ApplicationBuilder(session, application);
    }

    /**
     * Create a new builder around an existing application. In this case,
     * only the application's ID is used, as the entity may be a member of a
     * different session. Note that this will try to populate some of the
     * internal fields with what's already been loaded into the database, so
     * missing fields may cause errors.
     *
     * @param id      The application id.
     * @param session A database session to use, from which entities should
     *                be resolved.
     * @return A new context builder.
     */
    public static ApplicationBuilder fromApplication(final Session session,
                                                     final BigInteger id) {
        // Start a transaction.
        session.getTransaction().begin();

        // Find various pieces of data that can be easily accessed.
        Application application = session.get(Application.class, id);
        Map<String, ApplicationScope> scopes = application.getScopes();
        ApplicationScope scope = scopes.values().iterator().next();
        Client client = application.getClients().get(0);

        // If we can find a client with a token, switch to that instead.
        List<Client> clients = application.getClients().stream()
                .flatMap(c -> c.getTokens().stream())
                .map(OAuthToken::getClient)
                .collect(Collectors.toList());
        if (clients.size() > 0) {
            client = clients.get(0);
        }

        Authenticator authenticator = client.getAuthenticators().get(0);
        User user = application.getUsers().get(0);

        Role role = null;
        if (application.getRoles().size() > 0) {
            role = application.getRoles().get(0);
        }

        UserIdentity userIdentity = null;
        if (user.getIdentities().size() > 0) {
            userIdentity = user.getIdentities().get(0);
        }

        OAuthToken token = null;
        if (client.getTokens().size() > 0) {
            token = client.getTokens().get(0);
        }

        // We're done with the session, close the transaction.
        session.getTransaction().commit();

        // Build the context.
        ApplicationContext context = new ApplicationContext(session);
        context.application = application;
        context.scopes.putAll(scopes);
        context.scope = scope;
        context.client = client;
        context.authenticator = authenticator;
        context.role = role;
        context.userIdentity = userIdentity;
        context.token = token;

        return new ApplicationBuilder(context);
    }

    /**
     * Get the builder that was used to create this context.
     *
     * @return The builder.
     */
    public ApplicationContext getContext() {
        return context.copy();
    }

    /**
     * Add a role to this application.
     *
     * @param name The name of the role.
     * @return This environment builder.
     */
    public ApplicationBuilder role(final String name) {
        List<String> scopeNames = new ArrayList<>();
        if (context.scope != null) {
            scopeNames.add(context.scope.getName());
        }
        return role(name, scopeNames);
    }

    /**
     * Add a role to this application that is permitted a specific list of
     * scopes.
     *
     * @param name   The name of the role.
     * @param scopes The scopes to token (must already exist).
     * @return This environment builder.
     */
    public ApplicationBuilder role(final String name, final String[] scopes) {
        return role(name, new ArrayList<>(Arrays.asList(scopes)));
    }

    /**
     * Add a role to this application that is permitted a specific list of
     * scopes.
     *
     * @param name       The name of the role.
     * @param scopeNames The scope names to add to the role.
     * @return This environment builder.
     */
    public ApplicationBuilder role(final String name,
                                   final List<String> scopeNames) {
        context.role = new Role();
        context.role.setName(name);
        context.role.setApplication(context.application);

        // Attach all the scopes.
        for (String scopeName : scopeNames) {
            if (context.scopes.containsKey(scopeName)) {
                ApplicationScope scope = context.scopes.get(scopeName);
                context.role.getScopes().put(scopeName, scope);
            }
        }

        persist(context.role);

        // If the application doesn't have a default role, create it.
        if (context.application.getDefaultRole() == null) {
            context.application.setDefaultRole(context.role);
            persist(context.application);
        }

        return this;
    }

    /**
     * Add a scope to this application.
     *
     * @param name The name of the scope.
     * @return This environment builder.
     */
    public ApplicationBuilder scope(final String name) {

        context.scope = new ApplicationScope();
        context.scope.setName(name);
        context.scopes.put(name, context.scope);
        context.scope.setApplication(context.application);

        persist(context.scope);

        return this;
    }

    /**
     * Add a list of scopes to this application.
     *
     * @param scopes The list of scopes to add.
     * @return This builder.
     */
    public ApplicationBuilder scopes(final List<String> scopes) {
        for (String scope : scopes) {
            scope(scope);
        }
        return this;
    }

    /**
     * Add a client to this application.
     *
     * @param type The client type.
     * @return This builder.
     */
    public ApplicationBuilder client(final ClientType type) {
        return client(type, false);
    }

    /**
     * Add a client to this application.
     *
     * @param type The client type.
     * @param name An explicit client name.
     * @return This builder.
     */
    public ApplicationBuilder client(final ClientType type,
                                     final String name) {
        return client(type, name, false);
    }

    /**
     * Add a client, with a secret, to this application.
     *
     * @param isPrivate Is this a private client or not?
     * @param type      The client type.
     * @return This builder.
     */
    public ApplicationBuilder client(final ClientType type,
                                     final Boolean isPrivate) {
        return client(type, "Test Client", isPrivate);
    }

    /**
     * Add a client, with a name, to this application.
     *
     * @param isPrivate Is this a private client or not?
     * @param name      An explicit client name.
     * @param type      The client type.
     * @return This builder.
     */
    public ApplicationBuilder client(final ClientType type,
                                     final String name,
                                     final Boolean isPrivate) {
        context.client = new Client();
        context.client.setName(name);
        context.client.setType(type);
        context.client.setApplication(context.application);

        if (isPrivate) {
            context.client.setClientSecret(IdUtil.toString(IdUtil.next()));
        }

        persist(context.client);

        return this;
    }

    /**
     * Add a random redirect to the current client context.
     *
     * @return This builder.
     */
    public ApplicationBuilder redirect() {
        String rawUrl = String.format("http://%s/redirect",
                RandomStringUtils.randomAlphabetic(10));

        return redirect(rawUrl);
    }

    /**
     * Add a redirect to the current client context.
     *
     * @param redirect The Redirect URI for the client.
     * @return This builder.
     */
    public ApplicationBuilder redirect(final String redirect) {
        context.redirect = new ClientRedirect();
        context.redirect.setClient(context.client);
        context.redirect.setUri(UriBuilder.fromUri(redirect).build());

        persist(context.redirect);

        return this;
    }

    /**
     * Add a random referrer to the current client context.
     *
     * @return This builder.
     */
    public ApplicationBuilder referrer() {
        String rawUrl = String.format("http://%s/referrer",
                RandomStringUtils.randomAlphabetic(10));

        return referrer(rawUrl);
    }

    /**
     * Add a referrer to the current client context.
     *
     * @param referrer The Referral URI for the client.
     * @return This builder.
     */
    public ApplicationBuilder referrer(final String referrer) {
        context.referrer = new ClientReferrer();
        context.referrer.setClient(context.client);
        context.referrer.setUri(UriBuilder.fromUri(referrer).build());

        persist(context.referrer);

        return this;
    }

    /**
     * Enable an authenticator for the current client context.
     *
     * @param type The authenticator type to use.
     * @return This builder.
     */
    public ApplicationBuilder authenticator(final AuthenticatorType type) {

        context.authenticator = new Authenticator();
        context.authenticator.setType(type);
        context.authenticator.setClient(context.client);

        persist(context.authenticator);

        return this;
    }

    /**
     * Enable an authenticator for the current client context, with provided
     * configuration.
     *
     * @param type   The authenticator type to use.
     * @param config The configuration properties.
     * @return This builder.
     */
    public ApplicationBuilder authenticator(final AuthenticatorType type,
                                            final Map<String, String> config) {

        context.authenticator = new Authenticator();
        context.authenticator.setType(type);
        context.authenticator.setClient(context.client);
        context.authenticator.setConfiguration(config);

        persist(context.authenticator);

        return this;
    }

    /**
     * Create a new user for this application.
     *
     * @return This builder.
     */
    public ApplicationBuilder user() {
        return user(context.role);
    }

    /**
     * Create a new user with a specific role.
     *
     * @param role The role.
     * @return This builder.
     */
    public ApplicationBuilder user(final Role role) {
        context.user = new User();
        context.user.setRole(role);
        context.user.setApplication(context.application);

        persist(context.user);

        return this;
    }

    /**
     * Add a login for the current user context.
     *
     * @param login    The user login.
     * @param password The user password.
     * @return This builder.
     */
    public ApplicationBuilder login(final String login, final String password) {
        context.userIdentity = new UserIdentity();
        context.userIdentity.setRemoteId(login);
        context.userIdentity.setSalt(PasswordUtil.createSalt());
        context.userIdentity.setPassword(PasswordUtil.hash(password,
                context.userIdentity.getSalt()));
        context.userIdentity.setUser(context.user);
        context.userIdentity.setType(context.authenticator.getType());

        context.user.getIdentities().add(context.userIdentity);

        persist(context.user);
        persist(context.userIdentity);

        return this;
    }

    /**
     * Add an identity with a specific name to the current user context.
     *
     * @param remoteIdentity The unique identity.
     * @return This builder.
     */
    public ApplicationBuilder identity(final String remoteIdentity) {
        context.userIdentity = new UserIdentity();
        context.userIdentity.setRemoteId(remoteIdentity);
        context.userIdentity.setUser(context.user);
        context.userIdentity.setType(context.authenticator.getType());

        context.user.getIdentities().add(context.userIdentity);

        persist(context.user);
        persist(context.userIdentity);

        return this;
    }

    /**
     * Add an identity to the current user context.
     *
     * @return This builder.
     */
    public ApplicationBuilder identity() {
        return identity(IdUtil.toString(IdUtil.next()));
    }

    /**
     * Add an authorization code to the current client/redirect scope.
     *
     * @return This builder.
     */
    public ApplicationBuilder authToken() {
        return token(OAuthTokenType.Authorization, false, null,
                context.redirect.getUri().toString(), null);
    }

    /**
     * Add a bearer token to this user.
     *
     * @return This builder.
     */
    public ApplicationBuilder bearerToken() {
        return bearerToken((String[]) null);
    }

    /**
     * Add a scoped bearer token to this user.
     *
     * @param scopes The scopes to assign to this token.
     * @return This builder.
     */
    public ApplicationBuilder bearerToken(final String scopes) {
        return token(OAuthTokenType.Bearer, false, scopes, null, null);
    }

    /**
     * Add a scoped bearer token for a specific user..
     *
     * @param client   The client to apply this to.
     * @param identity The identity to apply this to.
     * @param scopes   The scopes to assign to this token.
     * @return This builder.
     */
    public ApplicationBuilder bearerToken(final Client client,
                                          final UserIdentity identity,
                                          final String scopes) {
        return token(client, identity, OAuthTokenType.Bearer, false,
                scopes, null, null);
    }

    /**
     * Add a scoped bearer token to this user.
     *
     * @param scopes The scopes to assign to this token.
     * @return This builder.
     */
    public ApplicationBuilder bearerToken(final String... scopes) {
        return token(OAuthTokenType.Bearer,
                false,
                scopes != null
                        ? String.join(" ", (CharSequence[]) scopes)
                        : null,
                null, null);
    }

    /**
     * Add a scoped bearer token to this user.
     *
     * @param client The client for which to create this token.
     * @param scopes The scopes to assign to this token.
     * @return This builder.
     */
    public ApplicationBuilder bearerToken(final Client client,
                                          final String... scopes) {
        return token(client, context.userIdentity,
                OAuthTokenType.Bearer,
                false,
                String.join(" ", (CharSequence[]) scopes),
                null, null);
    }

    /**
     * Add a refresh token.
     *
     * @return This builder.
     */
    public ApplicationBuilder refreshToken() {
        String scopes = "";
        if (context.token != null) {
            scopes = String.join(" ", context.token.getScopes().keySet());
        }
        return token(Refresh, false, scopes, null, context.token);
    }

    /**
     * Customize a token.
     *
     * @param type        The token type.
     * @param expired     Whether it's expired.
     * @param scopeString The requested scope.
     * @param redirect    The redirect URL.
     * @param authToken   An optional auth token.
     * @return This builder.
     */
    public ApplicationBuilder token(final OAuthTokenType type,
                                    final Boolean expired,
                                    final String scopeString,
                                    final String redirect,
                                    final OAuthToken authToken) {
        return token(context.client, context.userIdentity, type, expired,
                scopeString, redirect, authToken);
    }

    /**
     * Customize a token.
     *
     * @param client      The client for which this token should be created.
     * @param identity    The identity for this token.
     * @param type        The token type.
     * @param expired     Whether it's expired.
     * @param scopeString The requested scope.
     * @param redirect    The redirect URL.
     * @param authToken   An optional auth token.
     * @return This builder.
     */
    public ApplicationBuilder token(final Client client,
                                    final UserIdentity identity,
                                    final OAuthTokenType type,
                                    final Boolean expired,
                                    final String scopeString,
                                    final String redirect,
                                    final OAuthToken authToken) {
        context.token = new OAuthToken();
        context.token.setTokenType(type);
        context.token.setClient(client);
        context.token.setIssuer("localhost");

        // Link to auth token
        if (authToken != null) {
            context.token.setAuthToken(authToken);
        }

        // Only non-client-credentials clients are associated with users.
        if (!context.token.getClient()
                .getType().equals(ClientCredentials)) {
            context.token.setIdentity(identity);
        }

        if (!StringUtils.isEmpty(redirect)) {
            URI redirectUri = UriBuilder.fromUri(redirect).build();
            context.token.setRedirect(redirectUri);
        }

        // If expired, else use defaults.
        if (expired) {
            context.token.setExpiresIn(-100);
        } else {
            context.token.setExpiresIn(100);
        }

        // Split and attach the scopes.
        SortedMap<String, ApplicationScope> newScopes = new TreeMap<>();
        SortedMap<String, ApplicationScope> currentScopes = context.scopes;
        if (!StringUtils.isEmpty(scopeString)) {
            for (String scope : scopeString.split(" ")) {
                newScopes.put(scope, currentScopes.get(scope));
            }
        }
        context.token.setScopes(newScopes);

        persist(context.token);
        return this;
    }

    /**
     * Add an identity claim.
     *
     * @param name  Name of the field.
     * @param value Value of the field.
     * @return This builder.
     */
    public ApplicationBuilder claim(final String name, final String value) {
        context.userIdentity.getClaims().putIfAbsent(name, value);
        persist(context.userIdentity);
        return this;
    }

    /**
     * Create an authenticator state on the present client.
     *
     * @return This builder.
     */
    public ApplicationBuilder authenticatorState() {
        context.authenticatorState = new AuthenticatorState();
        context.authenticatorState.setClientRedirect(context.redirect.getUri());
        context.authenticatorState.setAuthenticator(context.authenticator);

        persist(context.authenticatorState);
        return this;
    }

    /**
     * Set the owner for the current application.
     *
     * @param user The new owner.
     * @return This builder.
     */
    public ApplicationBuilder owner(final User user) {
        context.application.setOwner(user);
        persist(context.application);
        return this;
    }

    /**
     * Create a new http session.
     *
     * @param expired Whether this session is expired.
     * @return This builder.
     */
    public ApplicationBuilder httpSession(final Boolean expired) {

        context.httpSession = new HttpSession();
        context.httpSession.setSessionTimeout(expired ? -100 : 100);

        if (context.token != null
                && context.token.getTokenType().equals(Refresh)) {
            context.token.setHttpSession(context.httpSession);
        }

        persist(context.httpSession);

        if (context.token != null
                && context.httpSession.equals(context.token.getHttpSession())) {
            persist(context.token);
        }

        return this;
    }

    /**
     * Track the entity for later flushing to the database.
     *
     * @param e The entity to persist.
     */
    public void persist(final AbstractEntity e) {
        // Set created/updated dates for all entities.
        if (e.getCreatedDate() == null) {
            e.setCreatedDate(Calendar.getInstance(UTC));
        }
        e.setModifiedDate(Calendar.getInstance(UTC));

        if (!trackedEntities.contains(e)) {
            trackedEntities.add(e);
        }
    }

    /**
     * Persist the requested changes to the database.
     *
     * @return The constructed context, detached from the hibernate session.
     */
    public ApplicationContext build() {
        Session session = context.session;

        // Persist/update
        session.getTransaction().begin();
        for (AbstractEntity e : trackedEntities) {
            session.saveOrUpdate(e);
        }
        session.getTransaction().commit();

        // Refresh
        session.getTransaction().begin();
        for (AbstractEntity e : trackedEntities) {
            session.refresh(e);
        }
        session.getTransaction().commit();

        return this.context.copy();
    }

    /**
     * A snapshot of the current application state, accessible and queryable
     * without having to access a database session.
     */
    public static final class ApplicationContext {

        /**
         * The session used to persist this data.
         */
        private final Session session;
        /**
         * The HTTP session.
         */
        private HttpSession httpSession;
        /**
         * The current application context.
         */
        private Application application;
        /**
         * The most recent created scope.
         */
        private ApplicationScope scope;
        /**
         * The current application scopes.
         */
        private SortedMap<String, ApplicationScope> scopes = new TreeMap<>();
        /**
         * An authenticator state.
         */
        private AuthenticatorState authenticatorState;
        /**
         * The current role context.
         */
        private Role role;
        /**
         * The current client context.
         */
        private Client client;
        /**
         * The current authenticator context.
         */
        private Authenticator authenticator;
        /**
         * The user context.
         */
        private User user;
        /**
         * The user identity context.
         */
        private UserIdentity userIdentity;
        /**
         * The oauth token context.
         */
        private OAuthToken token;
        /**
         * The last redirect created.
         */
        private ClientRedirect redirect;
        /**
         * The last referrer created.
         */
        private ClientReferrer referrer;

        /**
         * Create a new context instance.
         *
         * @param session The session used to persist these entities.
         */
        private ApplicationContext(final Session session) {
            this.session = session;
        }

        /**
         * Get the builder that was used to create this context.
         *
         * @return The builder.
         */
        public ApplicationBuilder getBuilder() {
            return new ApplicationBuilder(this.copy());
        }

        /**
         * Get the current http session.
         *
         * @return The current session.
         */
        public HttpSession getHttpSession() {
            return httpSession;
        }

        /**
         * Get the current http session.
         *
         * @return The current session.
         */
        public String getHttpSessionId() {
            if (httpSession != null) {
                return IdUtil.toString(httpSession.getId());
            }
            return "";
        }

        /**
         * Get the current application.
         *
         * @return The current application.
         */
        public Application getApplication() {
            return application;
        }

        /**
         * Get the current role.
         *
         * @return The current role.
         */
        public Role getRole() {
            return role;
        }

        /**
         * Return the current list of active scopes.
         *
         * @return The list of scopes.
         */
        public SortedMap<String, ApplicationScope> getScopes() {
            return Collections.unmodifiableSortedMap(scopes);
        }

        /**
         * Get the current client.
         *
         * @return The current client.
         */
        public Client getClient() {
            return client;
        }

        /**
         * Get the current redirect.
         *
         * @return The current redirect.
         */
        public ClientRedirect getRedirect() {
            return redirect;
        }

        /**
         * Get the current referrer.
         *
         * @return The current referrer.
         */
        public ClientReferrer getReferrer() {
            return referrer;
        }

        /**
         * Get the current authenticator.
         *
         * @return The current authenticator.
         */
        public Authenticator getAuthenticator() {
            return authenticator;
        }

        /**
         * Get the current user.
         *
         * @return The current user.
         */
        public User getUser() {
            return user;
        }

        /**
         * Get the current user identity.
         *
         * @return The current user identity.
         */
        public UserIdentity getUserIdentity() {
            return userIdentity;
        }

        /**
         * Get the current token.
         *
         * @return The current token.
         */
        public OAuthToken getToken() {
            return token;
        }

        /**
         * Get the current scope.
         *
         * @return The current scope.
         */
        public ApplicationScope getScope() {
            return scope;
        }

        /**
         * Get the owner of this app.
         *
         * @return The application owner.
         */
        public User getOwner() {
            return getApplication().getOwner();
        }

        /**
         * Return the current authenticator state.
         *
         * @return The authenticator state.
         */
        public AuthenticatorState getAuthenticatorState() {
            return authenticatorState;
        }

        /**
         * Take a snapshot of the current built context and return it.
         *
         * @return A snapshot of the current builder context.
         */
        protected ApplicationContext copy() {
            ApplicationContext snapshot = new ApplicationContext(session);
            snapshot.application = application;
            snapshot.client = client;
            snapshot.authenticator = authenticator;
            snapshot.scope = scope;
            snapshot.scopes = scopes;
            snapshot.authenticatorState = authenticatorState;
            snapshot.role = role;
            snapshot.user = user;
            snapshot.userIdentity = userIdentity;
            snapshot.token = token;
            snapshot.redirect = redirect;
            snapshot.referrer = referrer;
            snapshot.httpSession = httpSession;

            return snapshot;
        }
    }
}
