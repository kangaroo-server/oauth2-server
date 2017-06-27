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

package net.krotscheck.kangaroo.authz.admin;


import net.krotscheck.kangaroo.authz.admin.v1.filter.OAuth2AuthorizationFilter;
import net.krotscheck.kangaroo.authz.admin.v1.resource.ApplicationService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.AuthenticatorService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.ClientService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.OAuthTokenService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.RoleScopeService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.RoleService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.ScopeService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.UserIdentityService;
import net.krotscheck.kangaroo.authz.admin.v1.resource.UserService;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.FirstRunContainerLifecycleListener;
import net.krotscheck.kangaroo.authz.admin.v1.servlet.ServletConfigFactory;
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorFeature;
import net.krotscheck.kangaroo.authz.common.cors.AuthzCORSFeature;
import net.krotscheck.kangaroo.authz.common.database.DatabaseFeature;
import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.kangaroo.common.logging.LoggingFeature;
import net.krotscheck.kangaroo.common.security.SecurityFeature;
import net.krotscheck.kangaroo.common.status.StatusFeature;
import net.krotscheck.kangaroo.common.timedtasks.TimedTasksFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;


/**
 * The OID Servlet application, including all configured resources and
 * features.
 *
 * @author Michael Krotscheck
 */
public final class AdminV1API extends ResourceConfig {

    /**
     * Constructor. Creates a new application instance.
     */
    public AdminV1API() {
        // No autodiscovery, we load everything explicitly.
        property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);

        // Ensure that role annotations are respected.
        register(RolesAllowedDynamicFeature.class);

        // Common features.
        register(LoggingFeature.class);          // Logging Configuration
        register(ConfigurationFeature.class);    // Configuration loader
        register(JacksonFeature.class);          // Data Type de/serialization.
        register(ExceptionFeature.class);        // Exception Mapping.
        register(DatabaseFeature.class);         // Database Feature.
        register(StatusFeature.class);           // Heartbeat service.
        register(SecurityFeature.class);         // Security components.
        register(TimedTasksFeature.class);       // Timed tasks service.
        register(AuthenticatorFeature.class);    // OAuth2 Authenticators
        register(AuthzCORSFeature.class);        // CORS feature.

        // Internal components
        register(new ServletConfigFactory.Binder());
        register(new FirstRunContainerLifecycleListener.Binder());

        // API Authorization
        register(new OAuth2AuthorizationFilter.Binder());

        // API Resources
        register(ApplicationService.class);
        register(AuthenticatorService.class);
        register(ScopeService.class);
        register(ClientService.class);
        register(RoleService.class);
        register(UserService.class);
        register(UserIdentityService.class);
        register(OAuthTokenService.class);

        // API Subresources
        register(new RoleScopeService.Binder());
    }
}