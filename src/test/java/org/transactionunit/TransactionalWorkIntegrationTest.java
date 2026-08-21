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

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.transactionunit.TransactionUnitProvider.PERSISTENCE_PROVIDER_PROPERTY;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

/**
 * Tests {@link TransactionUnitEntityManagerFactory#runInTransaction(java.util.function.Consumer)} and
 * {@link TransactionUnitEntityManagerFactory#callInTransaction(java.util.function.Function)} through their
 * behavior. Delegating this work to the persistence provider would really commit it, so these tests would
 * see the data of each other.
 */
@RollbackAfterTest
public class TransactionalWorkIntegrationTest {

    private static final int DEADLOCK_TIMEOUT_SECONDS = 10;

    private static TransactionUnitEntityManagerFactory entityManagerFactory;

    @BeforeAll
    public static void createEntityManagerFactory() {
        Map<String, String> persistenceProperties = new HashMap<>();
        persistenceProperties.put("jakarta.persistence.provider", TransactionUnitProvider.class.getName());
        persistenceProperties.put(PERSISTENCE_PROVIDER_PROPERTY, HibernatePersistenceProvider.class.getName());
        entityManagerFactory
            = (TransactionUnitEntityManagerFactory)Persistence.createEntityManagerFactory("test-unit", persistenceProperties);
    }

    @Test
    @DisplayName("work of the first test is rolled back")
    public void firstTransactionalWorkIsRolledBack() {
        assertThat(countUsers()).isZero();

        entityManagerFactory.runInTransaction(entityManager -> entityManager.persist(new TestUser("First")));

        assertThat(countUsers()).isEqualTo(1);
    }

    @Test
    @DisplayName("work of the second test is rolled back")
    public void secondTransactionalWorkIsRolledBack() {
        assertThat(countUsers()).isZero();

        entityManagerFactory.runInTransaction(entityManager -> entityManager.persist(new TestUser("Second")));

        assertThat(countUsers()).isEqualTo(1);
    }

    @Test
    @DisplayName("callInTransaction returns the result of the work")
    public void callInTransactionReturnsResult() {
        entityManagerFactory.runInTransaction(entityManager -> entityManager.persist(new TestUser("Result")));

        String name = entityManagerFactory.callInTransaction(
            entityManager -> entityManager.createQuery("SELECT u.name FROM TestUser u", String.class).getSingleResult());

        assertThat(name).isEqualTo("Result");
    }

    @Test
    @DisplayName("the factory stays usable after transactional work")
    public void factoryStaysUsableAfterTransactionalWork() {
        entityManagerFactory.runInTransaction(entityManager -> entityManager.persist(new TestUser("Reused")));

        // the entity manager of the factory must not be left behind closed by the work
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList()).hasSize(1);
        }

        entityManagerFactory.runInTransaction(entityManager -> entityManager.persist(new TestUser("ReusedAgain")));

        assertThat(countUsers()).isEqualTo(2);
    }

    @Test
    @DisplayName("transactional work joins an already active transaction of the same thread")
    public void transactionalWorkJoinsActiveTransaction() {
        // the whole scenario runs in the timeout thread, because holding the entity manager is bound to the thread
        assertTimeoutPreemptively(ofSeconds(DEADLOCK_TIMEOUT_SECONDS), () -> {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            try {
                EntityManager workEntityManager = entityManagerFactory.callInTransaction(nested -> {
                    nested.persist(new TestUser("Joined"));
                    return nested;
                });

                // the same entity manager and the same transaction are used, so the transaction is still open
                assertThat(workEntityManager).isSameAs(entityManager);
                assertThat(entityManager.getTransaction().isActive()).isTrue();
                assertThat(entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList()).hasSize(1);
            } finally {
                entityManager.close();
            }
        });
    }

    @Test
    @DisplayName("work that marks the transaction for rollback is not flushed")
    public void rollbackOnlyWorkIsNotFlushed() {
        assertThat(countUsers()).isZero();

        entityManagerFactory.runInTransaction(entityManager -> {
            entityManager.persist(new TestUser("RollbackOnly"));
            entityManager.getTransaction().setRollbackOnly();
        });

        assertThat(entityManagerFactory.isRollbackOnly()).isTrue();
    }

    @Test
    @DisplayName("the entity manager is released when the work closes it")
    public void entityManagerIsReleasedWhenWorkClosesIt() {
        entityManagerFactory.runInTransaction(EntityManager::close);

        // the permit must not be leaked, otherwise the next entity manager would block forever
        assertTimeoutPreemptively(ofSeconds(DEADLOCK_TIMEOUT_SECONDS), () -> {
            try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
                assertThat(entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList()).isEmpty();
            }
        });
    }

    @Test
    @DisplayName("the entity manager is released when the work fails")
    public void entityManagerIsReleasedWhenWorkFails() {
        RuntimeException failure = new RuntimeException("work failed");

        assertThatThrownBy(() -> entityManagerFactory.runInTransaction(entityManager -> {
            throw failure;
        })).isSameAs(failure);

        assertTimeoutPreemptively(ofSeconds(DEADLOCK_TIMEOUT_SECONDS), () -> {
            try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
                assertThat(entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList()).isEmpty();
            }
        });
    }

    private int countUsers() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            return entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList().size();
        }
    }
}
