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

import static org.transactionunit.TransactionUnitProvider.getInstance;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

public class TransactionUnitEntityManagerFactory implements EntityManagerFactory {

    private static final Logger LOG = Logger.getLogger(TransactionUnitEntityManagerFactory.class.getName());

    private EntityManagerFactory delegate;
    private Semaphore entityManagerSemaphore = new Semaphore(1);
    private TransactionUnitEntityManager entityManager;

    public TransactionUnitEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        delegate = entityManagerFactory;
        getInstance().registerEntityManagerFactory(this);
    }

    public void close() {
        getInstance().unregisterEntityManagerFactory(this);
        EntityManagerFactory factory = delegate;
        delegate = null;
        factory.close();
    }

    public EntityManager createEntityManager() {
        return createEntityManager(delegate::createEntityManager);
    }

    public EntityManager createEntityManager(Map map) {
        return createEntityManager(() -> delegate.createEntityManager(map));
    }

    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        return createEntityManager(() -> delegate.createEntityManager(synchronizationType));
    }

    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        return createEntityManager(() -> delegate.createEntityManager(synchronizationType, map));
    }

    private EntityManager createEntityManager(Supplier<EntityManager> delegateSupplier) {
        entityManagerSemaphore.acquireUninterruptibly();
        if (entityManager == null) {
            entityManager = new TransactionUnitEntityManager(this, delegateSupplier.get());
        }
        return entityManager;
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

    void rollbackAll() {
        if (entityManagerSemaphore.availablePermits() == 0) {
            LOG.info("Stale EntityManager found, releasing");
            release();
        }

        if (entityManager != null) {
            entityManager.rollbackAndClose();
            entityManager = null;
        }
    }

    void release() {
        entityManagerSemaphore.release();
    }
}
