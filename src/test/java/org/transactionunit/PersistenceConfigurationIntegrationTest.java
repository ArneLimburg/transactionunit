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

import static org.assertj.core.api.Assertions.assertThat;
import static org.transactionunit.TransactionUnitProvider.PERSISTENCE_PROVIDER_PROPERTY;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;

/**
 * Tests the programmatic bootstrap that JPA 3.2 introduced with {@link PersistenceConfiguration}.
 * <p>
 * The persistence unit names used here are deliberately absent from {@code META-INF/persistence.xml}, so the
 * entity manager factories can only be built from the managed classes and properties of the configuration. That
 * proves that the configuration is really handed over to the delegate.
 */
@RollbackAfterTest
public class PersistenceConfigurationIntegrationTest {

    @Test
    @DisplayName("a programmatic configuration is wrapped and rolls back after the test")
    public void configuredEntityManagerFactoryRollsBack() {
        EntityManagerFactory createdEntityManagerFactory = configuration("programmatic-unit")
            .property(PERSISTENCE_PROVIDER_PROPERTY, HibernatePersistenceProvider.class.getName())
            .createEntityManagerFactory();

        assertThat(createdEntityManagerFactory).isInstanceOf(TransactionUnitEntityManagerFactory.class);
        TransactionUnitEntityManagerFactory entityManagerFactory
            = (TransactionUnitEntityManagerFactory)createdEntityManagerFactory;

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList()).isEmpty();
            entityManager.getTransaction().begin();
            entityManager.persist(new TestUser("Programmatic"));
            entityManager.getTransaction().commit();
        }

        // a commit inside the test only flushes, so the user is visible for the rest of the test
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList()).hasSize(1);
        }

        entityManagerFactory.rollbackAll();

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList()).isEmpty();
        }

        entityManagerFactory.rollbackAll();
        entityManagerFactory.close();
    }

    @Test
    @DisplayName("the delegate is guessed when only TransactionUnit itself is configured as provider")
    public void delegateIsGuessedWithoutDelegateProperty() {
        // Without the delegate property, the only provider named by the configuration is TransactionUnit itself.
        // Using that as the delegate would recurse until the stack overflows, so the real provider is guessed.
        // A fresh provider is used, so that a delegate cached by another test cannot hide the recursion.
        EntityManagerFactory createdEntityManagerFactory
            = new TransactionUnitProvider().createEntityManagerFactory(configuration("guessed-unit"));

        assertThat(createdEntityManagerFactory).isInstanceOf(TransactionUnitEntityManagerFactory.class);
        TransactionUnitEntityManagerFactory entityManagerFactory
            = (TransactionUnitEntityManagerFactory)createdEntityManagerFactory;

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            entityManager.getTransaction().begin();
            entityManager.persist(new TestUser("Guessed"));
            entityManager.getTransaction().commit();
            assertThat(entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList()).hasSize(1);
        }

        entityManagerFactory.rollbackAll();
        entityManagerFactory.close();
    }

    @Test
    @DisplayName("a foreign provider named by the configuration is used as delegate")
    public void configuredProviderIsUsedAsDelegate() {
        // here the configuration names the real provider, so it can be used as the delegate without recursing
        PersistenceConfiguration configuration
            = configuration("delegating-unit").provider(HibernatePersistenceProvider.class.getName());

        EntityManagerFactory createdEntityManagerFactory = new TransactionUnitProvider().createEntityManagerFactory(configuration);

        assertThat(createdEntityManagerFactory).isInstanceOf(TransactionUnitEntityManagerFactory.class);
        TransactionUnitEntityManagerFactory entityManagerFactory
            = (TransactionUnitEntityManagerFactory)createdEntityManagerFactory;

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            entityManager.getTransaction().begin();
            entityManager.persist(new TestUser("Delegated"));
            entityManager.getTransaction().commit();
            assertThat(entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList()).hasSize(1);
        }

        entityManagerFactory.rollbackAll();
        entityManagerFactory.close();
    }

    private PersistenceConfiguration configuration(String unitName) {
        return new PersistenceConfiguration(unitName)
            .provider(TransactionUnitProvider.class.getName())
            .managedClass(TestUser.class)
            .property("jakarta.persistence.jdbc.driver", "org.h2.Driver")
            .property("jakarta.persistence.jdbc.url", "jdbc:h2:mem:" + unitName)
            .property("jakarta.persistence.jdbc.user", "sa")
            .property("jakarta.persistence.jdbc.password", "")
            .property("jakarta.persistence.schema-generation.database.action", "drop-and-create")
            .property("hibernate.jpa.compliance.transaction", "true");
    }
}
