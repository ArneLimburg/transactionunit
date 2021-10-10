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

import static io.github.arnelimburg.transactionunit.RollbackAfterTest.Type.METHOD;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@RollbackAfterTest(METHOD)
public class TransactionUnitEntityManagerTest {

    @Test
    public void lifecycleMethodsAreDelegated() {

        EntityManager delegate = mock(EntityManager.class);
        EntityTransaction transaction = mock(EntityTransaction.class);
        when(delegate.getTransaction()).thenReturn(transaction);
        TransactionUnitEntityManager entityManager = new TransactionUnitEntityManager(() -> delegate);

        entityManager.persist(new Object());
        verify(delegate).persist(any(Object.class));

        entityManager.merge(new Object());
        verify(delegate).merge(any(Object.class));

        entityManager.remove(new Object());
        verify(delegate).remove(any(Object.class));

        entityManager.lock(new Object(), LockModeType.NONE);
        verify(delegate).lock(any(Object.class), eq(LockModeType.NONE));

        entityManager.lock(new Object(), LockModeType.NONE, emptyMap());
        verify(delegate).lock(any(Object.class), eq(LockModeType.NONE), any(Map.class));

        entityManager.refresh(new Object());
        verify(delegate).refresh(any(Object.class));

        entityManager.refresh(new Object(), emptyMap());
        verify(delegate).refresh(any(Object.class), any(Map.class));

        entityManager.refresh(new Object(), LockModeType.NONE);
        verify(delegate).refresh(any(Object.class), eq(LockModeType.NONE));

        entityManager.refresh(new Object(), LockModeType.NONE, emptyMap());
        verify(delegate).refresh(any(Object.class), eq(LockModeType.NONE), any(Map.class));

        entityManager.clear();
        verify(delegate).clear();

        entityManager.detach(new Object());
        verify(delegate).detach(any(Object.class));

        entityManager.contains(new Object());
        verify(delegate).contains(any(Object.class));

        entityManager.close();
    }

    @Test
    public void queryMethodsAreDelegated() {

        EntityManager delegate = mock(EntityManager.class);
        EntityTransaction transaction = mock(EntityTransaction.class);
        when(delegate.getTransaction()).thenReturn(transaction);
        TransactionUnitEntityManager entityManager = new TransactionUnitEntityManager(() -> delegate);

        entityManager.find(Class.class, new Object());
        verify(delegate).find(any(Class.class), Mockito.any(Object.class));

        entityManager.find(Class.class, new Object(), emptyMap());
        verify(delegate).find(any(Class.class), any(Object.class), any(Map.class));

        entityManager.find(Class.class, new Object(), LockModeType.NONE);
        verify(delegate).find(any(Class.class), any(Object.class), eq(LockModeType.NONE));

        entityManager.find(Class.class, new Object(), LockModeType.NONE, emptyMap());
        verify(delegate).find(any(Class.class), any(Object.class), eq(LockModeType.NONE), any(Map.class));

        entityManager.getReference(Object.class, new Object());
        verify(delegate).getReference(any(Class.class), any(Object.class));

        entityManager.createQuery("");
        verify(delegate).createQuery(anyString());

        entityManager.createQuery(mock(CriteriaQuery.class));
        verify(delegate).createQuery(any(CriteriaQuery.class));

        entityManager.createQuery(mock(CriteriaUpdate.class));
        verify(delegate).createQuery(any(CriteriaUpdate.class));

        entityManager.createQuery(mock(CriteriaDelete.class));
        verify(delegate).createQuery(any(CriteriaDelete.class));

        entityManager.createQuery("", Object.class);
        verify(delegate).createQuery(anyString(), eq(Object.class));

        entityManager.createNamedQuery("");
        verify(delegate).createNamedQuery(anyString());

        entityManager.createNamedQuery("", Object.class);
        verify(delegate).createNamedQuery(anyString(), eq(Object.class));

        entityManager.createNativeQuery("");
        verify(delegate).createNativeQuery(anyString());

        entityManager.createNativeQuery("", Object.class);
        verify(delegate).createNativeQuery(anyString(), eq(Object.class));

        entityManager.createNativeQuery("", "");
        verify(delegate).createNativeQuery(anyString(), anyString());

        entityManager.createNamedStoredProcedureQuery("");
        verify(delegate).createNamedStoredProcedureQuery(anyString());

        entityManager.createStoredProcedureQuery("");
        verify(delegate).createStoredProcedureQuery(anyString());

        entityManager.createStoredProcedureQuery("", Object.class, Object.class);
        verify(delegate).createStoredProcedureQuery("", Object.class, Object.class);

        entityManager.createStoredProcedureQuery("", "");
        verify(delegate).createStoredProcedureQuery("", "");

        entityManager.getCriteriaBuilder();
        verify(delegate).getCriteriaBuilder();

        entityManager.createEntityGraph(Object.class);
        verify(delegate).createEntityGraph(any(Class.class));

        entityManager.createEntityGraph("");
        verify(delegate).createEntityGraph(any(String.class));

        entityManager.getEntityGraph("");
        verify(delegate).getEntityGraph(any(String.class));

        entityManager.getEntityGraphs(Object.class);
        verify(delegate).getEntityGraphs(any(Class.class));

        entityManager.close();
    }

    @Test
    public void otherMethodsAreDelegated() {

        EntityManager delegate = mock(EntityManager.class);
        EntityTransaction transaction = mock(EntityTransaction.class);
        when(delegate.getTransaction()).thenReturn(transaction);
        TransactionUnitEntityManager entityManager = new TransactionUnitEntityManager(() -> delegate);

        entityManager.flush();
        verify(delegate).flush();

        entityManager.setFlushMode(FlushModeType.AUTO);
        verify(delegate).setFlushMode(FlushModeType.AUTO);

        entityManager.getFlushMode();
        verify(delegate).getFlushMode();

        entityManager.getLockMode(new Object());
        verify(delegate).getLockMode(any(Object.class));

        entityManager.setProperty("", new Object());
        verify(delegate).setProperty(anyString(), any(Object.class));

        entityManager.getProperties();
        verify(delegate).getProperties();

        entityManager.joinTransaction();
        verify(delegate).joinTransaction();

        entityManager.isJoinedToTransaction();
        verify(delegate).isJoinedToTransaction();

        entityManager.unwrap(Object.class);
        verify(delegate).unwrap(any(Class.class));

        entityManager.getDelegate();
        verify(delegate).getDelegate();

        entityManager.isOpen();
        verify(delegate).isOpen();

        entityManager.getTransaction();
        verify(delegate).getTransaction();

        entityManager.getEntityManagerFactory();
        verify(delegate).getEntityManagerFactory();

        entityManager.getMetamodel();
        verify(delegate).getMetamodel();

        entityManager.close();
    }
}
