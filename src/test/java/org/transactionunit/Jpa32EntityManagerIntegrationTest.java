/*
 * Copyright 2026 Arne Limburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.transactionunit;

import static jakarta.persistence.LockModeType.NONE;
import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.transactionunit.TransactionUnitProvider.PERSISTENCE_PROVIDER_PROPERTY;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.Root;

/**
 * Tests the API that JPA 3.2 added to {@link EntityManager} through the behavior of a real persistence provider.
 * Every test asserts an empty database first, so that each of them also proves that the previous test has been
 * rolled back.
 */
@RollbackAfterTest
public class Jpa32EntityManagerIntegrationTest {

    private static final int LOCK_TIMEOUT_MILLIS = 1000;
    private static final int TRANSACTION_TIMEOUT_SECONDS = 7;

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    public static void createEntityManagerFactory() {
        Map<String, String> persistenceProperties = new HashMap<>();
        persistenceProperties.put("jakarta.persistence.provider", TransactionUnitProvider.class.getName());
        persistenceProperties.put(PERSISTENCE_PROVIDER_PROPERTY, HibernatePersistenceProvider.class.getName());
        entityManagerFactory = Persistence.createEntityManagerFactory("test-unit", persistenceProperties);
    }

    @Test
    @DisplayName("find with FindOptions and an entity graph finds the flushed user")
    public void findWithOptions() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(findAll(entityManager)).isEmpty();
            entityManager.getTransaction().begin();
            TestUser user = persistAndFlush(entityManager, "Find");

            assertThat(entityManager.find(TestUser.class, user.getId(), CacheStoreMode.BYPASS)).isSameAs(user);
            assertThat(entityManager.find(TestUser.class, user.getId(), NONE)).isSameAs(user);
            assertThat(entityManager.find(TestUser.class, user.getId(), Timeout.ms(LOCK_TIMEOUT_MILLIS))).isSameAs(user);

            EntityGraph<TestUser> entityGraph = entityManager.createEntityGraph(TestUser.class);
            assertThat(entityManager.find(entityGraph, user.getId(), CacheStoreMode.BYPASS)).isSameAs(user);

            entityManager.getTransaction().commit();
        }
    }

    @Test
    @DisplayName("getReference of a managed entity refers to the same row")
    public void getReferenceOfEntity() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(findAll(entityManager)).isEmpty();
            entityManager.getTransaction().begin();
            TestUser user = persistAndFlush(entityManager, "Reference");

            assertThat(entityManager.getReference(user).getId()).isEqualTo(user.getId());

            entityManager.getTransaction().commit();
        }
    }

    @Test
    @DisplayName("lock with LockOptions keeps the user managed")
    public void lockWithOptions() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(findAll(entityManager)).isEmpty();
            entityManager.getTransaction().begin();
            TestUser user = persistAndFlush(entityManager, "Lock");

            entityManager.lock(user, PESSIMISTIC_WRITE, PessimisticLockScope.NORMAL);
            entityManager.lock(user, PESSIMISTIC_WRITE, Timeout.ms(LOCK_TIMEOUT_MILLIS));

            assertThat(entityManager.contains(user)).isTrue();
            // the exact mode a provider escalates a pessimistic lock to is not specified, but it must be locked
            assertThat(entityManager.getLockMode(user)).isNotEqualTo(NONE);

            entityManager.getTransaction().commit();
        }
    }

    @Test
    @DisplayName("refresh with RefreshOptions reloads the row that was changed via the connection")
    public void refreshWithOptions() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(findAll(entityManager)).isEmpty();
            entityManager.getTransaction().begin();
            TestUser user = persistAndFlush(entityManager, "BeforeRefresh");

            entityManager.runWithConnection((Connection connection) -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("UPDATE TestUser SET name = 'AfterRefresh' WHERE id = " + user.getId());
                }
            });
            // a LockModeType would bind to refresh(Object, LockModeType) instead of the RefreshOption varargs
            entityManager.refresh(user, CacheStoreMode.BYPASS);

            assertThat(user.getName()).isEqualTo("AfterRefresh");

            entityManager.getTransaction().commit();
        }
    }

    @Test
    @DisplayName("cache modes are read back from the delegate")
    public void cacheModesAreDelegated() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            entityManager.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);
            entityManager.setCacheStoreMode(CacheStoreMode.BYPASS);

            assertThat(entityManager.getCacheRetrieveMode()).isEqualTo(CacheRetrieveMode.BYPASS);
            assertThat(entityManager.getCacheStoreMode()).isEqualTo(CacheStoreMode.BYPASS);

            entityManager.setCacheRetrieveMode(CacheRetrieveMode.USE);
            entityManager.setCacheStoreMode(CacheStoreMode.USE);

            assertThat(entityManager.getCacheRetrieveMode()).isEqualTo(CacheRetrieveMode.USE);
            assertThat(entityManager.getCacheStoreMode()).isEqualTo(CacheStoreMode.USE);
        }
    }

    @Test
    @DisplayName("createQuery accepts a CriteriaSelect and a TypedQueryReference")
    public void createQueryWithNewQueryTypes() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(findAll(entityManager)).isEmpty();
            entityManager.getTransaction().begin();
            TestUser user = persistAndFlush(entityManager, "Query");

            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaSelect<TestUser> union = builder.union(selectAll(builder), selectNothing(builder));
            assertThat(entityManager.createQuery(union).getResultList()).containsExactly(user);

            TypedQueryReference<TestUser> namedQuery
                = entityManagerFactory.getNamedQueries(TestUser.class).get(TestUser.FIND_ALL);
            assertThat(entityManager.createQuery(namedQuery).getResultList()).containsExactly(user);

            entityManager.getTransaction().commit();
        }
    }

    @Test
    @DisplayName("runWithConnection and callWithConnection see the flushed, uncommitted row")
    public void connectionAccessSeesUncommittedData() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(findAll(entityManager)).isEmpty();
            entityManager.getTransaction().begin();
            persistAndFlush(entityManager, "Connection");

            entityManager.runWithConnection((Connection connection) -> assertThat(countUsers(connection)).isEqualTo(1));
            assertThat(entityManager.callWithConnection(this::countUsers)).isEqualTo(1);

            entityManager.getTransaction().commit();
        }
    }

    @Test
    @DisplayName("the transaction timeout is kept by the delegate, not by the wrapper")
    public void transactionTimeoutIsDelegated() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            entityManager.getTransaction().setTimeout(TRANSACTION_TIMEOUT_SECONDS);

            // getTransaction() returns a new wrapper on every call, so this only works if the delegate keeps the state
            assertThat(entityManager.getTransaction().getTimeout()).isEqualTo(TRANSACTION_TIMEOUT_SECONDS);
        }
    }

    private CriteriaQuery<TestUser> selectAll(CriteriaBuilder builder) {
        CriteriaQuery<TestUser> query = builder.createQuery(TestUser.class);
        query.select(query.from(TestUser.class));
        return query;
    }

    private CriteriaQuery<TestUser> selectNothing(CriteriaBuilder builder) {
        CriteriaQuery<TestUser> query = builder.createQuery(TestUser.class);
        Root<TestUser> user = query.from(TestUser.class);
        query.select(user).where(builder.equal(user.get("name"), "no-such-user"));
        return query;
    }

    private TestUser persistAndFlush(EntityManager entityManager, String name) {
        TestUser user = new TestUser(name);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private int countUsers(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM TestUser");
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private List<?> findAll(EntityManager entityManager) {
        return entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList();
    }
}
