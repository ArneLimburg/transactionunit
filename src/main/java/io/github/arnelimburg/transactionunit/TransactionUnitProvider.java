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

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

public class TransactionUnitProvider implements PersistenceProvider {

    private PersistenceProvider delegate;

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
        return getDelegate(map).createEntityManagerFactory(emName, map);
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        return getDelegate(map).createContainerEntityManagerFactory(info, map);
    }

    @Override
    public void generateSchema(PersistenceUnitInfo info, Map map) {
        getDelegate(map).generateSchema(info, map);
    }

    @Override
    public boolean generateSchema(String persistenceUnitName, Map map) {
        return getDelegate(map).generateSchema(persistenceUnitName, map);
    }

    @Override
    public ProviderUtil getProviderUtil() {
        if (delegate == null) {
            throw new IllegalStateException("No persistence provider initialized");
        }
        return delegate.getProviderUtil();
    }

    private PersistenceProvider getDelegate(Map map) {
        if (delegate == null) {
            String providerName = (String)map.get("io.github.arnelimburg.transactionunit.persistence.provider");
            if (providerName == null) {
                throw new IllegalStateException("Please specify 'io.github.arnelimburg.transactionunit.persistence.provider'");
            }
            try {
                delegate = (PersistenceProvider)Class.forName(providerName).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return delegate;
    }
}
