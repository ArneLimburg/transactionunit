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

import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

@Dependent
@Decorator
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class TransactionUnitEntityManagerFactory implements EntityManagerFactory {

    private static EntityManagerFactory delegate;

    @Inject
    public TransactionUnitEntityManagerFactory(@Delegate EntityManagerFactory entityManagerFactory) {
        if (delegate != null && delegate.isOpen()) {
            Logger.getLogger(TransactionUnitEntityManagerFactory.class.getName()).warning("Stale EntityManagerFactory found, closing...");
            delegate.close();
        }
        delegate = entityManagerFactory;
    }

    public void close() {
        EntityManagerFactory factory = delegate;
        delegate = null;
        factory.close();
    }

    public EntityManager createEntityManager() {
        return new TransactionUnitEntityManager(delegate::createEntityManager);
    }

    public EntityManager createEntityManager(Map map) {
        return new TransactionUnitEntityManager(() -> delegate.createEntityManager(map));
    }

    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        return new TransactionUnitEntityManager(() -> delegate.createEntityManager(synchronizationType));
    }

    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        return new TransactionUnitEntityManager(() -> delegate.createEntityManager(synchronizationType, map));
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return delegate.getCriteriaBuilder();
    }

    public Metamodel getMetamodel() {
        return delegate.getMetamodel();
    }

    public boolean isOpen() {
        return delegate.isOpen();
    }

    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    public Cache getCache() {
        return delegate.getCache();
    }

    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return delegate.getPersistenceUnitUtil();
    }

    public void addNamedQuery(String name, Query query) {
        delegate.addNamedQuery(name, query);
    }

    public <T> T unwrap(Class<T> cls) {
        return delegate.unwrap(cls);
    }

    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        delegate.addNamedEntityGraph(graphName, entityGraph);
    }
}
