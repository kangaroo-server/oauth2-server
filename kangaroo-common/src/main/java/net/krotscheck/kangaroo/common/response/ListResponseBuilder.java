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

package net.krotscheck.kangaroo.common.response;

import java.util.List;
import javax.ws.rs.core.Response;

/**
 * Convenience utility to build our common list response objects.
 *
 * @author Michael Krotscheck
 */
public final class ListResponseBuilder {

    /**
     * The wrapped builder.
     */
    private Response.ResponseBuilder builder = Response.ok();

    /**
     * Create a new personal response builder.
     *
     * @return The response builder.
     */
    public static ListResponseBuilder builder() {
        return new ListResponseBuilder();
    }

    /**
     * Add a list or results to the response.
     *
     * @param results Some results.
     * @return The builder.
     */
    public ListResponseBuilder addResult(final List<?> results) {
        builder.entity(results);
        return this;
    }

    /**
     * Add a limit to the response.
     *
     * @param limit The limit.
     * @return The builder.
     */
    public ListResponseBuilder limit(final Number limit) {
        builder.header(ApiParam.LIMIT_HEADER, limit.longValue());
        return this;
    }

    /**
     * Add a total to the response.
     *
     * @param total The total.
     * @return The builder.
     */
    public ListResponseBuilder total(final Number total) {
        builder.header(ApiParam.TOTAL_HEADER, total.longValue());
        return this;
    }

    /**
     * Add a total to the response.
     *
     * @param total The total.
     * @return The builder.
     */
    public ListResponseBuilder total(final Object total) {
        builder.header(ApiParam.TOTAL_HEADER, Long.valueOf(total.toString()));
        return this;
    }

    /**
     * Add an offset to the response.
     *
     * @param offset The offset.
     * @return The builder.
     */
    public ListResponseBuilder offset(final Number offset) {
        builder.header(ApiParam.OFFSET_HEADER, offset.longValue());
        return this;
    }

    /**
     * Add a sort to the response.
     *
     * @param sort The sort.
     * @return The builder.
     */
    public ListResponseBuilder sort(final String sort) {
        builder.header(ApiParam.SORT_HEADER, sort);
        return this;
    }

    /**
     * Add a sort order to the response.
     *
     * @param order The order.
     * @return The builder.
     */
    public ListResponseBuilder order(final SortOrder order) {
        builder.header(ApiParam.ORDER_HEADER, order.toString());
        return this;
    }

    /**
     * Add a sort order to the response.
     *
     * @param order The order.
     * @return The builder.
     */
    public ListResponseBuilder order(final String order) {
        return this.order(SortOrder.fromString(order));
    }

    /**
     * Build the response.
     *
     * @return The response.
     */
    public Response build() {
        // Apply the vary headers
        return builder.build();
    }
}
