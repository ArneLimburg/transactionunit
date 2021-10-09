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
package io.github.arnelimburg.transactionunit;

import static io.github.arnelimburg.transactionunit.TransactionUnitProvider.PERSISTENCE_PROVIDER_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SimpleJpaTest {

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    public static void createEntityManagerFactory() {
        Map<String, String> persistenceProperties = new HashMap<>();
        persistenceProperties.put("javax.persistence.provider", TransactionUnitProvider.class.getName());
        persistenceProperties.put(PERSISTENCE_PROVIDER_PROPERTY, HibernatePersistenceProvider.class.getName());
        entityManagerFactory = Persistence.createEntityManagerFactory("test-unit", persistenceProperties);
    }

    @Test
    public void multipleTransactions() {
        EntityManager entityManager;

        entityManager = entityManagerFactory.createEntityManager();
        assumeTrue(entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList().isEmpty());
        entityManager.close();

        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(new TestUser("John Doe"));
        entityManager.getTransaction().commit();
        entityManager.close();

        entityManager = entityManagerFactory.createEntityManager();
        assertEquals(1, entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList().size());
        entityManager.close();
    }
}
