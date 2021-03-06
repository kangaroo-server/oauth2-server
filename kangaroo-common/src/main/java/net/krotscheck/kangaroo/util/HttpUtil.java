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

package net.krotscheck.kangaroo.util;

import com.google.common.base.Strings;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Utility class for common http body and header actions.
 *
 * @author Michael Krotscheck
 */
public final class HttpUtil {

    /**
     * The character set we're using.
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    /**
     * Private constructor - utility class.
     */
    private HttpUtil() {

    }

    /**
     * Create a basic auth header, base64 encoded.
     *
     * @param login    The login.
     * @param password The password.
     * @return 'Basic base64(login + ':' + password)'
     */
    public static String authHeaderBasic(final String login,
                                         final String password) {
        if (Strings.isNullOrEmpty(login) || Strings.isNullOrEmpty(password)) {
            return "";
        }
        byte[] bytesEncoded =
                Base64.encodeBase64((login + ":" + password).getBytes(UTF8));
        return "Basic " + new String(bytesEncoded, UTF8);
    }

    /**
     * Create a basic auth header, base64 encoded.
     *
     * @param login    The login.
     * @param password The password.
     * @return 'Basic base64(login + ':' + password)'
     */
    public static String authHeaderBasic(final BigInteger login,
                                         final String password) {
        return authHeaderBasic(IdUtil.toString(login), password);
    }

    /**
     * Create a bearer authorization header.
     *
     * @param token The bearer token ID.
     * @return 'Bearer (token)'
     */
    public static String authHeaderBearer(final BigInteger token) {
        return authHeaderBearer(IdUtil.toString(token));
    }

    /**
     * Create a bearer authorization header.
     *
     * @param token The bearer token ID.
     * @return 'Bearer (token)'
     */
    public static String authHeaderBearer(final String token) {
        if (Strings.isNullOrEmpty(token)) {
            return "";
        }
        return String.format("Bearer %s", token);
    }

    /**
     * Helper method, which extracts the query string from a URI.
     *
     * @param uri The URI from which we're pulling the query response.
     * @return A map of all responses.
     */
    public static MultivaluedMap<String, String> parseQueryParams(
            final URI uri) {
        if (uri == null) {
            return new MultivaluedStringMap();
        }
        return parseQueryParams(uri.getRawQuery());
    }

    /**
     * Helper method, which extracts parameters from a query string.
     *
     * @param query The Query string to decode.
     * @return A map of all responses.
     */
    public static MultivaluedMap<String, String> parseQueryParams(
            final String query) {
        MultivaluedMap<String, String> results =
                new MultivaluedStringMap();
        List<NameValuePair> params = URLEncodedUtils
                .parse(query, Charset.forName("ISO-8859-1"));
        for (NameValuePair pair : params) {
            results.add(pair.getName(), pair.getValue());
        }
        return results;
    }

    /**
     * Helper method, which extracts the query string from a response body.
     *
     * @param response The HTTP query response to extract the body from.
     * @return A map of all variables in the inputstream.
     */
    public static MultivaluedMap<String, String> parseBodyParams(
            final Response response) {
        try {
            Charset charset = Charset.forName("UTF-8");
            InputStream stream = (InputStream) response.getEntity();
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer, charset);
            return parseQueryParams(writer.toString());
        } catch (IOException ioe) {
            logger.error("Could not decode params in stream.", ioe);
        }
        return new MultivaluedHashMap<>();
    }
}
