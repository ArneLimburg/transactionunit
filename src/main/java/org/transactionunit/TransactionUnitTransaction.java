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

import javax.persistence.EntityTransaction;

public class TransactionUnitTransaction implements EntityTransaction {

    private EntityTransaction delegate;
    private Runnable flushAction;

    public TransactionUnitTransaction(EntityTransaction transaction, Runnable flush) {
        delegate = transaction;
        flushAction = flush;
    }

    @Override
    public void begin() {
        if (!delegate.isActive()) {
            delegate.begin();
        }
    }

    @Override
    public void commit() {
        flushAction.run();
    }

    @Override
    public void rollback() {
        delegate.rollback();
    }

    @Override
    public void setRollbackOnly() {
        delegate.setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {
        return delegate.getRollbackOnly();
    }

    @Override
    public boolean isActive() {
        return delegate.isActive();
    }
}
