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

package net.krotscheck.features.database.entity;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.krotscheck.features.database.deserializer.AbstractEntityReferenceDeserializer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilterFactory;
import org.apache.lucene.analysis.miscellaneous.TrimFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * The user entity, as persisted to the database.
 *
 * @author Michael Krotscheck
 */
@Entity
@Table(name = "users")
@Indexed(index = "users")
@AnalyzerDef(name = "useranalyzer",
        charFilters = {
                @CharFilterDef(factory = HTMLStripCharFilterFactory.class)
        },
        tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
        filters = {
                @TokenFilterDef(factory = TrimFilterFactory.class),
                @TokenFilterDef(factory = LowerCaseFilterFactory.class),
                @TokenFilterDef(factory = StopFilterFactory.class),
                @TokenFilterDef(factory = SnowballPorterFilterFactory.class,
                        params = {
                                @Parameter(name = "language", value = "English")
                        }),
                @TokenFilterDef(
                        factory = RemoveDuplicatesTokenFilterFactory.class)
        })
public final class User extends AbstractEntity {

    /**
     * The Application to whom this user belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application", nullable = false, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = Application.Deserializer.class)
    private Application application;

    /**
     * The user's role in this application.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role", nullable = false, updatable = true)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = Role.Deserializer.class)
    private Role role;

    /**
     * List of this user's identities.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Cascade(CascadeType.ALL)
    @JsonIgnore
    @IndexedEmbedded(includePaths = {"claims"})
    private List<UserIdentity> identities;

    /**
     * Get the application this user belongs to.
     *
     * @return The application.
     */
    public Application getApplication() {
        return application;
    }

    /**
     * Set the application which this user belongs.
     *
     * @param application The new application.
     */
    public void setApplication(final Application application) {
        this.application = application;
    }

    /**
     * Get the role for this user.
     *
     * @return The user's role.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Set the role for this user.
     *
     * @param role The user's new role.
     */
    public void setRole(final Role role) {
        this.role = role;
    }

    /**
     * Get the identities for this user.
     *
     * @return A list of identities.
     */
    public List<UserIdentity> getIdentities() {
        return identities;
    }

    /**
     * Set the value for this user's identities.
     *
     * @param identities The new list of identities.
     */
    public void setIdentities(final List<UserIdentity> identities) {
        this.identities = new ArrayList<>(identities);
    }

    /**
     * Deserialize a reference to an User.
     *
     * @author Michael Krotschecks
     */
    public static final class Deserializer
            extends AbstractEntityReferenceDeserializer<User> {

        /**
         * Constructor.
         */
        public Deserializer() {
            super(User.class);
        }
    }
}
