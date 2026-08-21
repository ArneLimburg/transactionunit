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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import jakarta.persistence.Cache;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

public class TransactionUnitEntityManagerFactory implements EntityManagerFactory {

    private static final Logger LOG = Logger.getLogger(TransactionUnitEntityManagerFactory.class.getName());

    private EntityManagerFactory delegate;

    private Semaphore entityManagerSemaphore = new Semaphore(1);
    private volatile Thread entityManagerOwner;
    private TransactionUnitEntityManager entityManager;

    public TransactionUnitEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        delegate = entityManagerFactory;
        getInstance().registerEntityManagerFactory(this);
    }

    public void runInTransaction(Consumer<EntityManager> work) {
        callInTransaction(e -> {
            work.accept(e);
            return null;
        });
    }

    public <R> R callInTransaction(Function<EntityManager, R> work) {
        boolean nestedCall = holdsEntityManager();
        TransactionUnitEntityManager transactionUnitEntityManager = createEntityManager(delegate::createEntityManager);
        EntityTransaction transaction = transactionUnitEntityManager.getTransaction();
        boolean separateTransaction = !transaction.isActive();
        try {
            if (separateTransaction) {
                transaction.begin();
            }
            R result = work.apply(transactionUnitEntityManager);
            if (separateTransaction && !transaction.getRollbackOnly()) {
                transaction.commit();
            }
            return result;
        } finally {
            if (!nestedCall && !transactionUnitEntityManager.isClosed()) {
                release();
            }
        }
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

    private TransactionUnitEntityManager createEntityManager(Supplier<EntityManager> delegateSupplier) {
        if (!holdsEntityManager()) {
            entityManagerSemaphore.acquireUninterruptibly();
            entityManagerOwner = Thread.currentThread();
        }
        if (entityManager == null) {
            entityManager = new TransactionUnitEntityManager(this, delegateSupplier.get());
        } else {
            entityManager.reopen();
        }
        return entityManager;
    }

    private boolean holdsEntityManager() {
        return entityManagerOwner == Thread.currentThread();
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

    public String getName() {
        return delegate.getName();
    }

    public PersistenceUnitTransactionType getTransactionType() {
        return delegate.getTransactionType();
    }

    public SchemaManager getSchemaManager() {
        return delegate.getSchemaManager();
    }

    public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType) {
        return delegate.getNamedQueries(resultType);
    }

    public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityType) {
        return delegate.getNamedEntityGraphs(entityType);
    }

    public boolean isRollbackOnly() {
        if (entityManager != null) {
            return entityManager.getTransaction().getRollbackOnly();
        }
        return false;
    }

    public void rollbackAll() {
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
        entityManagerOwner = null;
        entityManagerSemaphore.release();
    }
}
