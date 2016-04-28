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

import net.krotscheck.features.config.SystemConfiguration;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

/**
 * Test the Service Version Filter, which attaches the current project version
 * to every HTTP Request.
 *
 * @author Michael Krotscheck
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SystemConfiguration.class)
public final class ServiceVersionFilterTest extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig a = new ResourceConfig();
        a.register(StatusFeature.class);
        return a;
    }

    /**
     * Assert that an injected version string is applied to the response
     * context.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testVersionAppliedToResponse() throws Exception {
        Response response = target("/")
                .request()
                .get();
        Assert.assertEquals("dev", response.getHeaderString("API-Version"));
    }
}
