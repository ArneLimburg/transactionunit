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

import static jakarta.persistence.PersistenceUnitTransactionType.RESOURCE_LOCAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.transactionunit.TransactionUnitProvider.PERSISTENCE_PROVIDER_PROPERTY;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Tests the metadata API that JPA 3.2 added to {@link EntityManagerFactory}. The values have to come from the
 * persistence unit that is configured in {@code META-INF/persistence.xml}, which proves that they are read from
 * the delegate instead of being made up by the wrapper.
 */
@RollbackAfterTest
public class Jpa32EntityManagerFactoryIntegrationTest {

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    public static void createEntityManagerFactory() {
        Map<String, String> persistenceProperties = new HashMap<>();
        persistenceProperties.put("jakarta.persistence.provider", TransactionUnitProvider.class.getName());
        persistenceProperties.put(PERSISTENCE_PROVIDER_PROPERTY, HibernatePersistenceProvider.class.getName());
        entityManagerFactory = Persistence.createEntityManagerFactory("test-unit", persistenceProperties);
    }

    @Test
    @DisplayName("the name of the persistence unit is taken from the delegate")
    public void nameIsDelegated() {
        assertThat(entityManagerFactory.getName()).isEqualTo("test-unit");
    }

    @Test
    @DisplayName("the transaction type of the persistence unit is taken from the delegate")
    public void transactionTypeIsDelegated() {
        assertThat(entityManagerFactory.getTransactionType()).isEqualTo(RESOURCE_LOCAL);
    }

    @Test
    @DisplayName("the schema manager of the delegate is handed out")
    public void schemaManagerIsDelegated() {
        assertThat(entityManagerFactory.getSchemaManager()).isNotNull();
    }

    @Test
    @DisplayName("the named query declared on the entity is found")
    public void namedQueriesAreDelegated() {
        assertThat(entityManagerFactory.getNamedQueries(TestUser.class)).containsKey(TestUser.FIND_ALL);
    }

    @Test
    @DisplayName("the named entity graph declared on the entity is found")
    public void namedEntityGraphsAreDelegated() {
        assertThat(entityManagerFactory.getNamedEntityGraphs(TestUser.class)).containsKey(TestUser.WITH_NAME);
    }
}
