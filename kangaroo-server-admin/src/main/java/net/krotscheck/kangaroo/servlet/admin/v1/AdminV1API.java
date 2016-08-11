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

package net.krotscheck.kangaroo.servlet.admin.v1;


import net.krotscheck.kangaroo.common.config.ConfigurationFeature;
import net.krotscheck.kangaroo.common.exception.ExceptionFeature;
import net.krotscheck.kangaroo.common.jackson.JacksonFeature;
import net.krotscheck.kangaroo.common.logging.LoggingFeature;
import net.krotscheck.kangaroo.common.status.StatusFeature;
import net.krotscheck.kangaroo.common.version.VersionFeature;
import net.krotscheck.kangaroo.database.DatabaseFeature;
import net.krotscheck.kangaroo.servlet.admin.v1.filter.OAuth2AuthorizationFilter;
import net.krotscheck.kangaroo.servlet.admin.v1.resource.ApplicationService;
import net.krotscheck.kangaroo.servlet.admin.v1.resource.ScopeService;
import net.krotscheck.kangaroo.servlet.admin.v1.resource.UserService;
import net.krotscheck.kangaroo.servlet.admin.v1.servlet.FirstRunContainerLifecycleListener;
import net.krotscheck.kangaroo.servlet.admin.v1.servlet.ServletConfigFactory;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;
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
        property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);

        // Ensure that role annotations are respected.
        register(RolesAllowedDynamicFeature.class);

        // Common features.
        register(LoggingFeature.class);          // Logging Configuration
        register(ConfigurationFeature.class);    // Configuration loader
        register(JacksonFeature.class);          // Data Type de/serialization.
        register(ExceptionFeature.class);        // Exception Mapping.
        register(DatabaseFeature.class);         // Database Feature.
        register(VersionFeature.class);          // Version response attachment.
        register(StatusFeature.class);           // Heartbeat service.

        // Internal components
        register(new ServletConfigFactory.Binder());
        register(new FirstRunContainerLifecycleListener.Binder());

        // API Authorization
        register(new OAuth2AuthorizationFilter.Binder());

        // API Services
        register(ApplicationService.class);
        register(ScopeService.class);
        register(UserService.class);
    }
}
