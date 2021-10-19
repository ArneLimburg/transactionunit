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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.transactionunit.RollbackAfterTest.Type.CLASS;
import static org.transactionunit.TransactionUnitProvider.PERSISTENCE_PROVIDER_PROPERTY;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@RollbackAfterTest(CLASS)
public class RollbackAfterClassTest {

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    public static void createEntityManagerFactory() {
        Map<String, String> persistenceProperties = new HashMap<>();
        persistenceProperties.put("javax.persistence.provider", TransactionUnitProvider.class.getName());
        persistenceProperties.put(PERSISTENCE_PROVIDER_PROPERTY, HibernatePersistenceProvider.class.getName());
        entityManagerFactory = Persistence.createEntityManagerFactory("test-unit", persistenceProperties);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(new TestUser("John Doe"));
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    @Test
    public void userIsFound() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        assertEquals(1, entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList().size());
        entityManager.close();
    }

    @AfterAll
    public static void userIsStillThere() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        assertEquals(1, entityManager.createNamedQuery(TestUser.FIND_ALL).getResultList().size());
        entityManager.close();
    }
}
