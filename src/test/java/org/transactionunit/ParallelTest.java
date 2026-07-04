/*
 * Copyright 2021 Arne Limburg
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

import jakarta.persistence.Persistence;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.transactionunit.TransactionUnitProvider.PERSISTENCE_PROVIDER_PROPERTY;

public class ParallelTest {


    public static TransactionUnitEntityManagerFactory createEntityManagerFactory() {
        Map<String, String> persistenceProperties = new HashMap<>();
        persistenceProperties.put("jakarta.persistence.provider", TransactionUnitProvider.class.getName());
        persistenceProperties.put(PERSISTENCE_PROVIDER_PROPERTY, HibernatePersistenceProvider.class.getName());
        return (TransactionUnitEntityManagerFactory) Persistence.createEntityManagerFactory("test-unit", persistenceProperties);
    }

    @Test
    public void testParallelTransactions() throws ExecutionException, InterruptedException {
        var entityManangerFactory = createEntityManagerFactory();
        entityManangerFactory.setParallelExecution(true);
        try {
            var executionContext1 = UUID.randomUUID().toString();
            var executionContext2 = UUID.randomUUID().toString();


            var user = new TestUser("John Doe");

            Executors.newSingleThreadExecutor().submit(
                    () -> {
                        entityManangerFactory.startExecutionContext(executionContext1);
                        var entityManager = entityManangerFactory.createEntityManager();
                        entityManager.getTransaction().begin();
                        entityManager.persist(user);
                        entityManager.getTransaction().commit();
                        entityManager.close();
                    }
            ).get();

            entityManangerFactory.startExecutionContext(executionContext2);
            var em1 = entityManangerFactory.createEntityManager();
            assertNull(em1.find(TestUser.class, user.getId()));
            em1.close();

            entityManangerFactory.startExecutionContext(executionContext1);
            var em2 = entityManangerFactory.createEntityManager();
            assertNotNull(em2.find(TestUser.class, user.getId()));
            em2.close();

        } finally {
            entityManangerFactory.rollbackAll();
            entityManangerFactory.close();
        }
    }


    @Test
    public void testSingleTransactions() throws ExecutionException, InterruptedException {
        var entityManangerFactory = createEntityManagerFactory();
        entityManangerFactory.setParallelExecution(false);
        try {

            var user = new TestUser("John Doe");

            Executors.newSingleThreadExecutor().submit(
                    () -> {
                        var entityManager = entityManangerFactory.createEntityManager();
                        entityManager.getTransaction().begin();
                        entityManager.persist(user);
                        entityManager.getTransaction().commit();
                        entityManager.close();
                    }
            ).get();

            assertNotNull(entityManangerFactory.createEntityManager().find(TestUser.class, user.getId()));

        } finally {
            entityManangerFactory.rollbackAll();
            entityManangerFactory.close();
        }
    }


}
