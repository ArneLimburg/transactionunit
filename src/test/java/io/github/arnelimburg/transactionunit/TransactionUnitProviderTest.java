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
import static java.util.Collections.singletonMap;
import static javax.persistence.SharedCacheMode.UNSPECIFIED;
import static javax.persistence.ValidationMode.AUTO;
import static javax.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TransactionUnitProviderTest {

    @Test
    @DisplayName("No ProviderUtil is available when no delegate is initialized")
    public void noProviderUtilWithoutDelegate() {
        assertThrows(IllegalStateException.class, () -> new TransactionUnitProvider().getProviderUtil());
    }

    @Test
    @DisplayName("No persistence provider")
    public void noPersistenceProvider() {
        assertThat(new TransactionUnitProvider().createEntityManagerFactory("test-unit", null)).isNull();
    }

    @Test
    @DisplayName("Wrong persistence provider")
    public void wrongPersistenceProvider() {
        assertThrows(IllegalStateException.class,
            () -> new TransactionUnitProvider().createEntityManagerFactory(
            "test-unit", singletonMap(PERSISTENCE_PROVIDER_PROPERTY, "NonExistingProvider")));
    }

    @Test
    @DisplayName("Hibernate ProviderUtil is used, when Hibernate Provider is used")
    public void hibernateProviderUtilWithHibernateProvider() {
        TransactionUnitProvider provider = new TransactionUnitProvider();
        EntityManagerFactory entityManagerFactory = provider.createContainerEntityManagerFactory(new TestPersistenceUnitInfo(), null);

        assertTrue(provider.getProviderUtil().getClass().getName().contains(".hibernate."));

        entityManagerFactory.close();
    }

    @Test
    @DisplayName("Hibernate Provider is used, when initialized with unit info")
    public void hibernateProviderWithUnitInfo() {
        TransactionUnitProvider provider = new TransactionUnitProvider();
        provider.createContainerEntityManagerFactory(new TestPersistenceUnitInfo(), null).close();

        EntityManagerFactory entityManagerFactory = provider.createEntityManagerFactory("test-unit", null);
        Assertions.assertNotNull(entityManagerFactory.unwrap(SessionFactory.class));
        entityManagerFactory.close();
    }

    @Test
    @DisplayName("generateSchema is delegated")
    public void generateSchema() {
        Map<String, String> persistenceProperties = new HashMap<>();
        persistenceProperties.put("javax.persistence.provider", TransactionUnitProvider.class.getName());
        persistenceProperties.put(PERSISTENCE_PROVIDER_PROPERTY, HibernatePersistenceProvider.class.getName());

        TransactionUnitProvider provider = new TransactionUnitProvider();
        provider.createContainerEntityManagerFactory(new TestPersistenceUnitInfo(), persistenceProperties);
        provider.generateSchema("test-unit", persistenceProperties);
        provider.generateSchema(new TestPersistenceUnitInfo(), persistenceProperties);
    }

    private static final class TestPersistenceUnitInfo implements PersistenceUnitInfo {

        @Override
        public String getPersistenceUnitName() {
            return "test-unit";
        }

        @Override
        public String getPersistenceProviderClassName() {
            return HibernatePersistenceProvider.class.getName();
        }

        @Override
        public PersistenceUnitTransactionType getTransactionType() {
            return RESOURCE_LOCAL;
        }

        @Override
        public DataSource getJtaDataSource() {
            return null;
        }

        @Override
        public DataSource getNonJtaDataSource() {
            return null;
        }

        @Override
        public List<String> getMappingFileNames() {
            return null;
        }

        @Override
        public List<URL> getJarFileUrls() {
            return null;
        }

        @Override
        public URL getPersistenceUnitRootUrl() {
            return null;
        }

        @Override
        public List<String> getManagedClassNames() {
            return null;
        }

        @Override
        public boolean excludeUnlistedClasses() {
            return false;
        }

        @Override
        public SharedCacheMode getSharedCacheMode() {
            return UNSPECIFIED;
        }

        @Override
        public ValidationMode getValidationMode() {
            return AUTO;
        }

        @Override
        public Properties getProperties() {
            Properties properties = new Properties();
            properties.put("javax.persistence.jdbc.driver", "org.h2.Driver");
            properties.put("javax.persistence.jdbc.url", "jdbc:h2:mem:test");
            properties.put("javax.persistence.jdbc.user", "sa");
            properties.put("javax.persistence.jdbc.password", "");
            properties.put("javax.persistence.jdbc.schema-generation.database.action", "drop-and-create");
            properties.put("hibernate.jpa.compliance.transaction", "true");
            return properties;
        }

        @Override
        public String getPersistenceXMLSchemaVersion() {
            return null;
        }

        @Override
        public ClassLoader getClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }

        @Override
        public void addTransformer(ClassTransformer transformer) {
        }

        @Override
        public ClassLoader getNewTempClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }
    }
}
