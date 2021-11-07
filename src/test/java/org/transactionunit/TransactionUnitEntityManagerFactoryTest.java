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

import static java.util.Collections.emptyMap;
import static javax.persistence.SynchronizationType.UNSYNCHRONIZED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;

import org.junit.jupiter.api.Test;

public class TransactionUnitEntityManagerFactoryTest {

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
}
