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

package net.krotscheck.api.status;

import net.krotscheck.features.config.ConfigurationFeature;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * This feature exposes a simple /status API endpoint that returns the current
 * version of the application.
 *
 * @author Michael Krotscheck
 */
public final class StatusFeature implements Feature {

    /**
     * Register this feature.
     */
    @Override
    public boolean configure(final FeatureContext context) {

        if (!context.getConfiguration()
                .isRegistered(ConfigurationFeature.class)) {
            context.register(ConfigurationFeature.class);
        }

        // Add the configuration injector
        context.register(new ServiceVersionFilter.Binder());

        // Add the /status resource
        context.register(StatusService.class);

        return true;
    }
}