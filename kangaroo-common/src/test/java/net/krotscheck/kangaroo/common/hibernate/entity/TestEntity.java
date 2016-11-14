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

package net.krotscheck.kangaroo.common.hibernate.entity;


import org.hibernate.search.annotations.Indexed;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A test hibernate persistence entity.
 *
 * @author Michael Krotscheck
 */
@Entity
@Indexed
@Table(name = "test")
public final class TestEntity {

    /**
     * Identifier.
     */
    @Id
    private Long id;

    /**
     * Get the ID.
     *
     * @return the ID.
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the ID.
     *
     * @param entityId The ID.
     */
    public void setId(final Long entityId) {
        this.id = entityId;
    }
}
