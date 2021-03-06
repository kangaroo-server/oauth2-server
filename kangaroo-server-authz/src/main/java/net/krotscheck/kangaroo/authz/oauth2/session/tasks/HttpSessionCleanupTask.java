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

package net.krotscheck.kangaroo.authz.oauth2.session.tasks;

import net.krotscheck.kangaroo.common.hibernate.migration.DatabaseMigrationState;
import net.krotscheck.kangaroo.common.timedtasks.RepeatingTask;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.type.LongType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.TimerTask;

/**
 * This timed task cleans up expired HTTP sessions.
 *
 * @author Michael Krotscheck
 */
public final class HttpSessionCleanupTask
        extends TimerTask
        implements RepeatingTask {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory
            .getLogger(HttpSessionCleanupTask.class);

    /**
     * The session factory.
     */
    private final SessionFactory sessionFactory;

    /**
     * The migration state of the database, this ensures that the task only
     * runs if the DB has been bootstrapped.
     */
    @SuppressWarnings("PMD.UnusedPrivateField")
    private final DatabaseMigrationState migrationState;

    /**
     * Create a new instance of this task, with injected parameters.
     *
     * @param sessionFactory The hibernate session factory, from which we
     *                       grab database sessions.
     * @param dbState        The database migration state, used to indicate
     *                       whether the DB is stable enough to run this query.
     */
    @Inject
    public HttpSessionCleanupTask(final SessionFactory sessionFactory,
                                  final DatabaseMigrationState dbState) {
        this.sessionFactory = sessionFactory;
        this.migrationState = dbState;
    }

    /**
     * The task to execute.
     *
     * @return The task to execute.
     */
    @Override
    public TimerTask getTask() {
        return this;
    }

    /**
     * Time in milliseconds between successive task executions.
     *
     * @return Time in milliseconds between successive task executions.
     */
    @Override
    public long getPeriod() {
        return 10 * 60 * 1000; // Every 10 minutes.
    }

    /**
     * The action to be performed by this timer task.
     */
    @Override
    public void run() {
        Session session = sessionFactory.openSession();
        Transaction t = session.beginTransaction();

        long deletedCount = 0;
        try {
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

            // Add a buffer, because tick sync.
            now.add(Calendar.MINUTE, -1);
            Long timestamp = now.getTimeInMillis();

            String hql = "DELETE FROM HttpSession"
                    + " WHERE (modifiedDate + (sessionTimeout * 1000))"
                    + " < :timestamp";

            deletedCount = session.createQuery(hql)
                    .setParameter("timestamp", timestamp, LongType.INSTANCE)
                    .executeUpdate();
            t.commit();
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
            t.rollback();
        } finally {
            session.close();
        }

        if (deletedCount > 0) {
            logger.debug(String.format("%s expired http sessions deleted.",
                    deletedCount));
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(HttpSessionCleanupTask.class)
                    .to(RepeatingTask.class)
                    .in(Singleton.class);
        }
    }
}
