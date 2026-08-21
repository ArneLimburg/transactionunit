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

import static java.util.Optional.ofNullable;
import static jakarta.persistence.spi.PersistenceProviderResolverHolder.getPersistenceProviderResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;

public class TransactionUnitProvider implements PersistenceProvider {

    public static final String PERSISTENCE_PROVIDER_PROPERTY = "org.transactionunit.persistence.provider";
    private static volatile TransactionUnitProvider instance;
    private List<TransactionUnitEntityManagerFactory> entityManagerFactories = new CopyOnWriteArrayList<>();
    private PersistenceProvider delegate;

    public TransactionUnitProvider() {
        instance = this;
    }

    public static TransactionUnitProvider getInstance() {
        TransactionUnitProvider local = instance;
        if (local != null) {
            return local;
        }
        return getPersistenceProviderResolver()
            .getPersistenceProviders()
            .stream()
            .filter(TransactionUnitProvider.class::isInstance)
            .map(TransactionUnitProvider.class::cast)
            .findAny()
            .orElseThrow(transactionUnitProviderNotFound());
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
        return getDelegate(map)
                .map(d -> d.createEntityManagerFactory(emName, filterProperties(map)))
                .map(TransactionUnitEntityManagerFactory::new)
                .orElse(null);
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration configuration) {
        Map mergedProperties = new HashMap<>(configuration.properties());
        // Unlike a PersistenceUnitInfo, a PersistenceConfiguration has a single provider only, and that provider
        // has to be this one for this provider to be called at all. Using it as the delegate as well would make
        // this method call itself until the stack overflows, so the real provider has to be guessed instead.
        if (!mergedProperties.containsKey(PERSISTENCE_PROVIDER_PROPERTY) && !isOwnProvider(configuration.provider())) {
            mergedProperties.put(PERSISTENCE_PROVIDER_PROPERTY, configuration.provider());
        }
        PersistenceProvider persistenceProvider = getDelegate(mergedProperties).orElseGet(this::initializeGuessedDelegate);
        PersistenceConfiguration configurationForDelegate
            = delegateConfiguration(configuration, persistenceProvider, filterProperties(mergedProperties));
        return ofNullable(persistenceProvider.createEntityManagerFactory(configurationForDelegate))
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
            return guessPersistenceProvider().getProviderUtil();
        }
        return delegate.getProviderUtil();
    }

    static Supplier<? extends IllegalStateException> transactionUnitProviderNotFound() {
        return () -> new IllegalStateException("TransactionUnitProvider not found");
    }

    public void rollbackAll() {
        entityManagerFactories.forEach(TransactionUnitEntityManagerFactory::rollbackAll);
    }

    void registerEntityManagerFactory(TransactionUnitEntityManagerFactory entityManagerFactory) {
        entityManagerFactories.add(entityManagerFactory);
    }

    void unregisterEntityManagerFactory(TransactionUnitEntityManagerFactory entityManagerFactory) {
        entityManagerFactories.remove(entityManagerFactory);
    }

    private Optional<PersistenceProvider> getDelegate(Map map) {
        if (delegate == null) {
            Optional<String> providerName
                = ofNullable(map).map(m -> (String)m.get(PERSISTENCE_PROVIDER_PROPERTY))
                .or(() -> ofNullable(System.getProperty(PERSISTENCE_PROVIDER_PROPERTY)))
                .or(() -> ofNullable(System.getenv(toEnvName(PERSISTENCE_PROVIDER_PROPERTY))));
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

    private static String toEnvName(String property) {
        return property.replace('.', '_').toUpperCase();
    }

    private boolean isOwnProvider(String providerClassName) {
        return getClass().getName().equals(providerClassName);
    }

    private PersistenceProvider initializeGuessedDelegate() {
        delegate = guessPersistenceProvider();
        return delegate;
    }

    private PersistenceProvider guessPersistenceProvider() {
        ServiceLoader<PersistenceProvider> persistenceProviders = ServiceLoader.load(PersistenceProvider.class);
        return persistenceProviders
                .stream()
                .filter(persistenceProviderProvider -> !persistenceProviderProvider.type().equals(getClass()))
                .map(ServiceLoader.Provider::get)
                .findAny()
                .orElseThrow(()-> new IllegalStateException("No persistence provider initialized"));
    }

    private Map<?, ?> filterProperties(Map<?, ?> properties) {
        Optional<String> persistenceProviderClassName = ofNullable(properties).map(p -> (String)p.get(PERSISTENCE_PROVIDER_PROPERTY));
        if (persistenceProviderClassName.isPresent()) {
            Map filteredProperties = new HashMap<>(properties);
            filteredProperties.put("jakarta.persistence.provider", persistenceProviderClassName.get());
            return filteredProperties;
        } else {
            return properties;
        }
    }

    private PersistenceConfiguration delegateConfiguration(
        PersistenceConfiguration configuration, PersistenceProvider persistenceProvider, Map<?, ?> properties) {
        PersistenceConfiguration configurationCopy = new PersistenceConfiguration(configuration.name())
            .provider(persistenceProvider.getClass().getName())
            .jtaDataSource(configuration.jtaDataSource())
            .nonJtaDataSource(configuration.nonJtaDataSource())
            .transactionType(configuration.transactionType())
            .sharedCacheMode(configuration.sharedCacheMode())
            .validationMode(configuration.validationMode())
            .properties((Map<String, ?>)properties);
        configuration.managedClasses().forEach(configurationCopy::managedClass);
        configuration.mappingFiles().forEach(configurationCopy::mappingFile);
        return configurationCopy;
    }
}
