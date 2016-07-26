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

package net.krotscheck.kangaroo.servlet.oauth2.util;

import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidRequestException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidScopeException;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.UnsupportedResponseType;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

/**
 * A utility filled with validation tools.
 *
 * @author Michael Krotscheck
 */
public final class ValidationUtil {

    /**
     * Utility class, private constructor.
     */
    private ValidationUtil() {

    }

    /**
     * Validate that a response type is appropriate for a given client.
     *
     * @param client       The client to check.
     * @param responseType The requested response type.
     */
    public static void validateResponseType(final Client client,
                                            final String responseType) {
        if (client != null) {
            if (ClientType.Implicit.equals(client.getType())
                    && "token".equals(responseType)) {
                return;
            } else if (ClientType.AuthorizationGrant.equals(client.getType())
                    && "code".equals(responseType)) {
                return;
            }
        }
        throw new UnsupportedResponseType();
    }

    /**
     * This method assists in determining if a particular URI is valid for
     * the scope of this client.
     *
     * @param redirect  The URI to check.
     * @param redirects A set of redirect url's to check against.
     * @return The validated redirect URI, or null.
     */
    public static URI validateRedirect(final String redirect,
                                       final Set<URI> redirects) {
        // Quick exit
        if (redirects.size() == 0) {
            throw new InvalidRequestException();
        }

        // Can we default?
        if (StringUtils.isEmpty(redirect)) {
            if (redirects.size() == 1) {
                URI[] redirectArray =
                        redirects.toArray(new URI[redirects.size()]);
                return redirectArray[0];
            } else {
                throw new InvalidRequestException();
            }
        }

        // Make sure the passed string is valid.
        URI redirectUri;
        try {
            redirectUri = UriBuilder.fromUri(redirect).build();
        } catch (Exception e) {
            throw new InvalidRequestException();
        }

        // Convert the query parameters into a multivaluedMap
        MultivaluedMap<String, String> params = extractParams(redirectUri);
        Set<String> keySet = new HashSet<>(params.keySet());
        params.keySet().retainAll(keySet);

        uriloop:
        for (URI test : redirects) {
            // Test the scheme
            if (!test.getScheme().equals(redirectUri.getScheme())) {
                continue;
            }
            // Test the host
            if (!test.getHost().equals(redirectUri.getHost())) {
                continue;
            }
            // Test the port
            if (test.getPort() != redirectUri.getPort()) {
                continue;
            }
            // Test the path
            if (!test.getPath().equals(redirectUri.getPath())) {
                continue;
            }

            MultivaluedMap<String, String> testParams = extractParams(test);
            keySet.addAll(testParams.keySet()); // This modifies 'params'.

            // All variables in testParams must exist in params to pass.
            for (Entry<String, List<String>> entry : testParams.entrySet()) {
                if (!params.get(entry.getKey()).containsAll(entry.getValue())) {
                    continue uriloop;
                }
            }

            return redirectUri; // NOPMD
        }
        throw new InvalidRequestException();
    }

    /**
     * Convert a URI and its query parameters into a MultivaluedMap, for
     * later comparison.
     *
     * @param redirectUri The URI to parse.
     * @return A map of all results.
     */
    private static MultivaluedMap<String, String> extractParams(
            final URI redirectUri) {
        MultivaluedMap<String, String> results = new MultivaluedHashMap<>();
        for (NameValuePair pair : URLEncodedUtils.parse(redirectUri, "UTF-8")) {
            results.add(pair.getName(), pair.getValue());
        }
        return results;
    }

    /**
     * Creates a collection of scopes from a list of valid scopes. If the
     * requested scopes are not in that valid list, it will throw an exception.
     *
     * @param requestedScopes An array of requested scopes.
     * @param validScopes     A list of valid scopes.
     * @return A list of the requested scopes, as database instances.
     */
    public static SortedMap<String, ApplicationScope> validateScope(
            final String[] requestedScopes,
            final SortedMap<String, ApplicationScope> validScopes) {

        if (requestedScopes == null || requestedScopes.length == 0) {
            return new TreeMap<>();
        }

        if (validScopes == null) {
            throw new InvalidScopeException();
        }

        // Make sure all requested scopes are in the map.
        SortedMap<String, ApplicationScope> results = new TreeMap<>();
        for (String scope : requestedScopes) {
            if (!validScopes.containsKey(scope)) {
                throw new InvalidScopeException();
            }
            results.put(scope, validScopes.get(scope));
        }
        return results;
    }

    /**
     * Creates a collection of scopes from a list of valid scopes. If the
     * requested scopes are not in that valid list, it will throw an exception.
     *
     * @param requestedScopes An array of requested scopes.
     * @param validScopes     A string of valid scopes.
     * @return A list of the requested scopes, as database instances.
     */
    public static SortedMap<String, ApplicationScope> validateScope(
            final String requestedScopes,
            final SortedMap<String, ApplicationScope> validScopes) {
        if (StringUtils.isEmpty(requestedScopes)) {
            return new TreeMap<>();
        }
        return validateScope(requestedScopes.split(" "), validScopes);
    }

    /**
     * Revalidates a list of provided scopes against the originally granted
     * scopes, as well as the current list of valid scopes. If the list of
     * valid scopes has changed since the original grant list, any missing
     * scopes will be quietly dropped.
     *
     * @param requestedScopes An array of requested scopes.
     * @param originalScopes  The original set of scopes.
     * @param validScopes     The current list of valid scopes.
     * @return A list of the requested scopes, as database instances.
     */
    public static SortedMap<String, ApplicationScope> revalidateScope(
            final String[] requestedScopes,
            final SortedMap<String, ApplicationScope> originalScopes,
            final SortedMap<String, ApplicationScope> validScopes) {

        if (validScopes == null || originalScopes == null) {
            throw new InvalidScopeException();
        }

        if (requestedScopes == null || requestedScopes.length == 0) {
            return new TreeMap<>();
        }

        // Reduce the valid scope list down by the original scopes.
        SortedMap<String, ApplicationScope> results = new TreeMap<>();
        for (String scope : requestedScopes) {
            if (!originalScopes.containsKey(scope)) {
                throw new InvalidScopeException();
            } else if (validScopes.containsKey(scope)) {
                results.put(scope, validScopes.get(scope));
            }
        }

        return results;
    }

    /**
     * Revalidates a list of provided scopes against the originally granted
     * scopes, as well as the current list of valid scopes. If the list of
     * valid scopes has changed since the original grant list, any missing
     * scopes will be quietly dropped.
     *
     * @param requestedScopes An array of requested scopes.
     * @param originalScopes  The original set of scopes.
     * @param validScopes     The current list of valid scopes.
     * @return A list of the requested scopes, as database instances.
     */
    public static SortedMap<String, ApplicationScope> revalidateScope(
            final String requestedScopes,
            final SortedMap<String, ApplicationScope> originalScopes,
            final SortedMap<String, ApplicationScope> validScopes) {
        if (StringUtils.isEmpty(requestedScopes)) {
            return new TreeMap<>();
        }
        return revalidateScope(requestedScopes.split(" "),
                originalScopes,
                validScopes);
    }

    /**
     * Ensure that an authenticator, requested by name, is valid within a
     * specific list of authenticators. If no string is provided, and yet the
     * list of authenticators only contains one, this will default to that
     * authenticator.
     *
     * @param authenticator  The requested authenticator string.
     * @param authenticators The list of authenticators to test against.
     * @return The valid authenticator.
     */
    public static Authenticator validateAuthenticator(
            final String authenticator,
            final List<Authenticator> authenticators) {
        // Quick exit
        if (authenticators.size() == 0) {
            throw new InvalidRequestException();
        }

        // Can we default?
        if (StringUtils.isEmpty(authenticator)) {
            if (authenticators.size() == 1) {
                return authenticators.get(0);
            } else {
                throw new InvalidRequestException();
            }
        }

        // Iterate through the set, comparing as we go.
        for (Authenticator test : authenticators) {
            if (test.getType().equals(authenticator)) {
                return test;
            }
        }
        throw new InvalidRequestException();
    }
}