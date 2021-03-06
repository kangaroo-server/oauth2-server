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

package net.krotscheck.kangaroo.common.hibernate.id;

import net.krotscheck.kangaroo.common.hibernate.type.BigIntegerType;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * This identity generator uses java's secureRandom to generate database
 * record ID's, checking the database for duplicates. The reasoning is that -
 * for HTTPSession Id's at least - that UUID's do not provide enough bits of
 * entropy to be secure. Given that, there's no real reason not to extend
 * this method to other entities, in order to keep things consistent.
 *
 * @author Michael Krotscheck
 */
public final class SecureRandomIdGenerator
        implements IdentifierGenerator, Configurable {

    /**
     * The JDBC SQL string to use when checking for ID conflicts.
     */
    private String sql;

    /**
     * The type instance, used for adjusting our prepared statements.
     */
    private Type type = new CustomType(new BigIntegerType());

    /**
     * Set a new SQL statement (used for testing).
     *
     * @param sql New statement.
     */
    protected void setSql(final String sql) {
        this.sql = sql;
    }

    /**
     * Generate a new identifier.
     *
     * @param session The session from which the request originates
     * @param object  the entity or collection (idbag) for which the id is being
     *                generated.
     * @return A new identifier.
     * @throws HibernateException Indicates trouble generating the identifier
     */
    @Override
    public Serializable generate(final SharedSessionContractImplementor session,
                                 final Object object)
            throws HibernateException {

        BigInteger nextId;
        do {
            nextId = IdUtil.next();
        } while (hasDuplicate(session, nextId));

        return nextId;
    }

    /**
     * Configure this generator for a specific type.
     *
     * @param type            The type.
     * @param params          The parameters.
     * @param serviceRegistry The included service registry.
     * @throws MappingException Not thrown.
     */
    @Override
    public void configure(final Type type,
                          final Properties params,
                          final ServiceRegistry serviceRegistry)
            throws MappingException {
        this.type = type;

        // Grab the column and table name.
        String column = params.getProperty(PersistentIdentifierGenerator.PK);
        String table = params.getProperty(PersistentIdentifierGenerator.TABLE);

        // Grab the JDBC environment managers.
        JdbcEnvironment jdbcEnvironment =
                serviceRegistry.getService(JdbcEnvironment.class);
        ObjectNameNormalizer normalizer = (ObjectNameNormalizer)
                params.get(PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER);

        // Render the column and table name for the JDBC environment
        column = normalizer.normalizeIdentifierQuoting(column)
                .render(jdbcEnvironment.getDialect());
        table = normalizer.toDatabaseIdentifierText(table);

        sql = String.format("select count(%s) from %s where %s=?",
                column, table, column);
    }

    /**
     * Returns true if a duplicate ID exists in this table, otherwise false.
     *
     * @param session The DB session to query against.
     * @param id      The ID to look for.
     * @return True or false.
     */
    protected boolean hasDuplicate(
            final SharedSessionContractImplementor session,
            final BigInteger id) {
        JdbcCoordinator coordinator = session.getJdbcCoordinator();
        LogicalConnectionImplementor logicalConnection =
                coordinator.getLogicalConnection();
        ResourceRegistry resourceRegistry =
                logicalConnection.getResourceRegistry();

        ResultSet resultSet = null;
        PreparedStatement st = coordinator
                .getStatementPreparer()
                .prepareStatement(sql);

        try {
            type.nullSafeSet(st, id, 1, session);

            resultSet = coordinator
                    .getResultSetReturn()
                    .extract(st);
            int count = 0;
            if (resultSet.next()) {
                count = resultSet.getInt(1);
            }
            resourceRegistry.release(resultSet, st);

            return count > 0;
        } catch (SQLException | GenericJDBCException sle) {
            throw new IdentifierGenerationException("Cannot scan for id", sle);
        } finally {
            resourceRegistry.release(st);
            coordinator.afterStatementExecution();
        }
    }
}
