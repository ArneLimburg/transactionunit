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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.jupiter.api.Test;

public class TransactionUnitTransactionTest {

    @Test
    public void methodsAreDelegated() {

        EntityManager entityManager = mock(EntityManager.class);
        EntityTransaction delegate = mock(EntityTransaction.class);
        TransactionUnitTransaction transaction = new TransactionUnitTransaction(delegate, entityManager::flush);

        transaction.rollback();
        verify(delegate).rollback();

        transaction.setRollbackOnly();
        verify(delegate).setRollbackOnly();

        transaction.isActive();
        verify(delegate).isActive();
    }
}
