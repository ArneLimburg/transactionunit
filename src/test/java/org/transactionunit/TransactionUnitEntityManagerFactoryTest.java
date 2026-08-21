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

import static jakarta.persistence.SynchronizationType.UNSYNCHRONIZED;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;

public class TransactionUnitEntityManagerFactoryTest {

    private static final long RELEASE_TIMEOUT_MILLIS = 10000;

    @Test
    public void allMethodsAreDelegated() {
        EntityManager entityManagerMock = mock(EntityManager.class);
        when(entityManagerMock.getTransaction()).thenReturn(mock(EntityTransaction.class));
        EntityManagerFactory delegate = mock(EntityManagerFactory.class);
        when(delegate.createEntityManager()).thenReturn(entityManagerMock);
        when(delegate.createEntityManager(any(Map.class))).thenReturn(entityManagerMock);
        when(delegate.createEntityManager(any(SynchronizationType.class))).thenReturn(entityManagerMock);
        when(delegate.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(entityManagerMock);
        TransactionUnitEntityManagerFactory entityManagerFactory = new TransactionUnitEntityManagerFactory(delegate);

        entityManagerFactory.createEntityManager();
        verify(delegate).createEntityManager();
        entityManagerFactory.rollbackAll();

        entityManagerFactory.createEntityManager(emptyMap());
        verify(delegate).createEntityManager(emptyMap());
        entityManagerFactory.rollbackAll();

        entityManagerFactory.createEntityManager(UNSYNCHRONIZED);
        verify(delegate).createEntityManager(UNSYNCHRONIZED);
        entityManagerFactory.rollbackAll();

        entityManagerFactory.createEntityManager(UNSYNCHRONIZED, emptyMap());
        verify(delegate).createEntityManager(UNSYNCHRONIZED, emptyMap());
        entityManagerFactory.rollbackAll();

        entityManagerFactory.getCriteriaBuilder();
        verify(delegate).getCriteriaBuilder();

        entityManagerFactory.getMetamodel();
        verify(delegate).getMetamodel();

        entityManagerFactory.isOpen();
        verify(delegate).isOpen();

        entityManagerFactory.getProperties();
        verify(delegate).getProperties();

        entityManagerFactory.getCache();
        verify(delegate).getCache();

        entityManagerFactory.getPersistenceUnitUtil();
        verify(delegate).getPersistenceUnitUtil();

        entityManagerFactory.addNamedQuery("", mock(Query.class));
        verify(delegate).addNamedQuery(anyString(), any(Query.class));

        entityManagerFactory.addNamedEntityGraph("", mock(EntityGraph.class));
        verify(delegate).addNamedEntityGraph(anyString(), any(EntityGraph.class));

        entityManagerFactory.unwrap(Class.class);
        verify(delegate).unwrap(Class.class);

        entityManagerFactory.close();
    }

    @Test
    @DisplayName("callInTransaction wraps the entity manager and releases it afterwards")
    public void callInTransactionReleasesEntityManager() {
        EntityManager entityManagerMock = mock(EntityManager.class);
        when(entityManagerMock.getTransaction()).thenReturn(mock(EntityTransaction.class));
        EntityManagerFactory delegate = mock(EntityManagerFactory.class);
        when(delegate.createEntityManager()).thenReturn(entityManagerMock);
        when(delegate.callInTransaction(any())).thenAnswer(invocation ->
            invocation.getArgument(0, Function.class).apply(entityManagerMock));
        TransactionUnitEntityManagerFactory entityManagerFactory = new TransactionUnitEntityManagerFactory(delegate);

        EntityManager transactionalEntityManager = entityManagerFactory.callInTransaction(entityManager -> entityManager);

        assertInstanceOf(TransactionUnitEntityManager.class, transactionalEntityManager);

        assertEntityManagerReleased(entityManagerFactory);

        entityManagerFactory.close();
    }

    @Test
    @DisplayName("runInTransaction releases the entity manager when the work fails")
    public void runInTransactionReleasesEntityManagerOnException() {
        EntityManager entityManagerMock = mock(EntityManager.class);
        when(entityManagerMock.getTransaction()).thenReturn(mock(EntityTransaction.class));
        EntityManagerFactory delegate = mock(EntityManagerFactory.class);
        when(delegate.createEntityManager()).thenReturn(entityManagerMock);
        when(delegate.callInTransaction(any())).thenAnswer(invocation ->
            invocation.getArgument(0, Function.class).apply(entityManagerMock));
        TransactionUnitEntityManagerFactory entityManagerFactory = new TransactionUnitEntityManagerFactory(delegate);

        RuntimeException failure = new RuntimeException("work failed");
        assertSame(failure, assertThrows(RuntimeException.class, () -> entityManagerFactory.runInTransaction(entityManager -> {
            throw failure;
        })));

        assertEntityManagerReleased(entityManagerFactory);

        entityManagerFactory.close();
    }

    /**
     * Acquires the next entity manager on a daemon thread, so that a leaked permit fails this test
     * instead of blocking the build forever. {@code @Timeout} cannot be used here, because Jupiter
     * runs the test in the calling thread and only detects the timeout once the test method returns,
     * which never happens for {@code Semaphore#acquireUninterruptibly()}.
     */
    private void assertEntityManagerReleased(TransactionUnitEntityManagerFactory entityManagerFactory) {
        Thread acquiringThread = new Thread(entityManagerFactory::createEntityManager);
        acquiringThread.setDaemon(true);
        acquiringThread.start();
        try {
            acquiringThread.join(RELEASE_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        assertFalse(acquiringThread.isAlive(), "the entity manager has not been released");
        entityManagerFactory.rollbackAll();
    }
}
