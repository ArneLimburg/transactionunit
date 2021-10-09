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

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

public class TransactionUnitProvider implements PersistenceProvider {

    public static final String PERSISTENCE_PROVIDER_PROPERTY = "io.github.arnelimburg.transactionunit.persistence.provider";
    private PersistenceProvider delegate;

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
        return getDelegate(map)
                .map(d -> d.createEntityManagerFactory(emName, filterProperties(map)))
                .map(TransactionUnitEntityManagerFactory::new)
                .orElse(null);
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        Map mergedProperties = new HashMap<>(info.getProperties());
        ofNullable(map).ifPresent(properties -> mergedProperties.putAll(properties));
        if (!mergedProperties.containsKey(PERSISTENCE_PROVIDER_PROPERTY)) {
            mergedProperties.put(PERSISTENCE_PROVIDER_PROPERTY, info.getPersistenceProviderClassName());
        }
        return getDelegate(mergedProperties)
                .map(d -> d.createContainerEntityManagerFactory(info, filterProperties(mergedProperties)))
                .map(TransactionUnitEntityManagerFactory::new)
                .orElse(null);
    }

    @Override
    public void generateSchema(PersistenceUnitInfo info, Map map) {
        getDelegate(map).ifPresent(d -> d.generateSchema(info, filterProperties(map)));
    }

    @Override
    public boolean generateSchema(String persistenceUnitName, Map map) {
        return getDelegate(map).map(d -> d.generateSchema(persistenceUnitName, filterProperties(map))).orElse(false);
    }

    @Override
    public ProviderUtil getProviderUtil() {
        if (delegate == null) {
            throw new IllegalStateException("No persistence provider initialized");
        }
        return delegate.getProviderUtil();
    }

    private Optional<PersistenceProvider> getDelegate(Map map) {
        if (delegate == null) {
            Optional<String> providerName
                = ofNullable(map).map(m -> (String)m.get(PERSISTENCE_PROVIDER_PROPERTY));
                //.orElseThrow(() -> new IllegalStateException("Please specify '" + PERSISTENCE_PROVIDER_PROPERTY + "'"));
            if (providerName.isPresent()) {
                try {
                    delegate = (PersistenceProvider)Class.forName(providerName.get()).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return Optional.ofNullable(delegate);
    }

    private Map<?, ?> filterProperties(Map<?, ?> properties) {
        Optional<String> persistenceProviderClassName = ofNullable(properties).map(p -> (String)p.get(PERSISTENCE_PROVIDER_PROPERTY));
        if (persistenceProviderClassName.isPresent()) {
            Map filteredProperties = new HashMap<>(properties);
            filteredProperties.put("javax.persistence.provider", persistenceProviderClassName.get());
            return filteredProperties;
        } else {
            return properties;
        }
    }
}
