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

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static org.transactionunit.TransactionUnitProvider.getInstance;

public class TransactionUnitEntityManagerFactory implements EntityManagerFactory {

    private static final Logger LOG = Logger.getLogger(TransactionUnitEntityManagerFactory.class.getName());

    private EntityManagerFactory delegate;
    private Map<String, Semaphore> entityManagerSemaphores = new HashMap<>();
    private Map<String, TransactionUnitEntityManager> entityManagers = new HashMap<>();
    private ThreadLocal<String> currentExecutionContext = new ThreadLocal<>();
    private boolean parallelExection = false;

    private static String DEFAULT_EXECUTION_CONTEXT = "TRANSACTIONUNIT_DEFAULT_EXECUTION_CONTEXT";

    public TransactionUnitEntityManagerFactory() {
    }

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

    public TransactionUnitEntityManager createEntityManager() {
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
        if (!this.parallelExection) {
            this.startExecutionContext(DEFAULT_EXECUTION_CONTEXT);
        }
        var executionContext = requireNonNull(currentExecutionContext.get(), "Execution context is null");
        this.entityManagerSemaphores.computeIfAbsent(this.currentExecutionContext.get(), key -> new Semaphore(1));
        getCurrentEntityManagerSemaphore().acquireUninterruptibly();
        return this.entityManagers.computeIfAbsent(executionContext,
                (ec) -> new TransactionUnitEntityManager(this, delegateSupplier.get(), ec));
    }

    private Semaphore getCurrentEntityManagerSemaphore() {
        return requireNonNull(this.entityManagerSemaphores.get(this.getCurrentExecutionContext()));
    }

    private String getCurrentExecutionContext() {
        if (!this.parallelExection) {
            return DEFAULT_EXECUTION_CONTEXT;
        } else {
            return currentExecutionContext.get();
        }
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

    public boolean isRollbackOnly() {
        var entityManager = this.getCurrentEnityManager();
        if (entityManager != null) {
            return entityManager.getTransaction().getRollbackOnly();
        }
        return false;
    }

    private TransactionUnitEntityManager getCurrentEnityManager() {
        return this.entityManagers.get(this.currentExecutionContext.get());
    }

    public void rollbackAll() {
        if (getCurrentEntityManagerSemaphore().availablePermits() == 0) {
            LOG.info("Stale EntityManager found, releasing");
            release();
        }

        for(var entityManager : this.entityManagers.values()) {
            entityManager.rollbackAndClose();
        }
        this.endAllExecutionContexts();
    }

    private void endAllExecutionContexts() {
        this.entityManagers.keySet().forEach(this::endExecutionContext);
    }

    void release() {
        getCurrentEntityManagerSemaphore().release();
    }

    public void startExecutionContext(String contextName) {
        var executionContext = this.currentExecutionContext.get();
//        if (executionContext != null) {
//            throw new RuntimeException("Execution context" + executionContext + " is still active.");
//        }
        this.currentExecutionContext.set(contextName);
    }

    private void endExecutionContext() {
        this.endExecutionContext(currentExecutionContext.get());
        currentExecutionContext.remove();
    }

    private void endExecutionContext(String executionContext) {
        this.entityManagers.remove(executionContext);
    }

    public void setParallelExecution(boolean value) {
        this.parallelExection = value;
    }
}
