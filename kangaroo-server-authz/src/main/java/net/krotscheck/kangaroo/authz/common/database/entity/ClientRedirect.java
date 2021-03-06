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

package net.krotscheck.kangaroo.authz.common.database.entity;

import org.hibernate.search.annotations.Indexed;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * This represents a redirect URL attached to a specific client.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "client_redirects")
@Indexed(index = "client_redirects")
public final class ClientRedirect extends AbstractClientUri {

}
