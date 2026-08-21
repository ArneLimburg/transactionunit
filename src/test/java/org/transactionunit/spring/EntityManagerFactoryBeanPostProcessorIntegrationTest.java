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
package org.transactionunit.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.transactionunit.RollbackAfterTest;
import org.transactionunit.TestUser;
import org.transactionunit.TransactionUnitEntityManagerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Tests the {@link EntityManagerFactoryBeanPostProcessor} against a real persistence provider. Spring may run a
 * post processor repeatedly and it initializes beans in an order the post processor cannot influence, so both
 * cases are driven explicitly here.
 */
@RollbackAfterTest
public class EntityManagerFactoryBeanPostProcessorIntegrationTest {

    private EntityManagerFactory delegate;
    private EntityManagerFactoryBeanPostProcessor postProcessor;

    @BeforeEach
    public void createDelegate() {
        Map<String, String> persistenceProperties = new HashMap<>();
        persistenceProperties.put("jakarta.persistence.schema-generation.database.action", "drop-and-create");
        // the delegate is created by the provider directly, so that it is not wrapped by TransactionUnit yet
        delegate = new HibernatePersistenceProvider().createEntityManagerFactory("test-unit", persistenceProperties);
        postProcessor = new EntityManagerFactoryBeanPostProcessor();
    }

    @AfterEach
    public void closeDelegate() {
        delegate.close();
    }

    @Test
    @DisplayName("an entity manager factory bean is wrapped")
    public void entityManagerFactoryIsWrapped() {
        Object bean = postProcessor.postProcessAfterInitialization(delegate, "entityManagerFactory");

        assertThat(bean).isInstanceOf(TransactionUnitEntityManagerFactory.class);
    }

    @Test
    @DisplayName("post processing the same factory repeatedly keeps one wrapper and one persistence context")
    public void repeatedPostProcessingKeepsOneWrapper() {
        Object wrapped = postProcessor.postProcessAfterInitialization(delegate, "entityManagerFactory");

        // Spring may hand the already wrapped bean back into the post processor
        Object wrappedAgain = postProcessor.postProcessAfterInitialization(wrapped, "entityManagerFactory");
        // and it may also post process the very same delegate again
        Object wrappedDelegateAgain = postProcessor.postProcessAfterInitialization(delegate, "entityManagerFactory");

        assertThat(wrappedAgain).isSameAs(wrapped);
        assertThat(wrappedDelegateAgain).isSameAs(wrapped);
    }

    @Test
    @DisplayName("a bean initialized after rollback only work triggers the rollback")
    public void rollbackOnlyIsRolledBackWhenAnotherBeanIsInitialized() {
        TransactionUnitEntityManagerFactory entityManagerFactory
            = (TransactionUnitEntityManagerFactory)postProcessor.postProcessAfterInitialization(delegate, "entityManagerFactory");

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(new TestUser("RollbackOnly"));
        entityManager.flush();
        entityManager.getTransaction().setRollbackOnly();

        assertThat(entityManagerFactory.isRollbackOnly()).isTrue();

        postProcessor.postProcessAfterInitialization("any other bean", "otherBean");

        try (EntityManager verification = entityManagerFactory.createEntityManager()) {
            assertThat(verification.createNamedQuery(TestUser.FIND_ALL).getResultList()).isEmpty();
        }
    }

    @Test
    @DisplayName("a bean initialized without pending rollback only work is returned unchanged")
    public void otherBeansArePassedThrough() {
        postProcessor.postProcessAfterInitialization(delegate, "entityManagerFactory");

        Object bean = new Object();

        assertThat(postProcessor.postProcessAfterInitialization(bean, "otherBean")).isSameAs(bean);
    }
}
