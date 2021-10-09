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
package io.github.arnelimburg.transactionunit.meecrowave;

import static io.github.arnelimburg.transactionunit.TransactionUnitProvider.PERSISTENCE_PROVIDER_PROPERTY;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;

import io.github.arnelimburg.transactionunit.TestUser;


@ApplicationScoped
public class TestRepository {

    @PersistenceContext(unitName = "test-unit", properties = {
        @PersistenceProperty(name = "javax.persistence.provider", value = "io.github.arnelimburg.transactionunit.TransactionUnitProvider"),
        @PersistenceProperty(name = PERSISTENCE_PROVIDER_PROPERTY, value = "org.hibernate.jpa.HibernatePersistenceProvider")} )
    EntityManager entityManager;

    public TestUser persistUser(TestUser user) {
        entityManager.persist(user);
        return user;
    }

    public List<TestUser> getAllUsers() {
        return entityManager.createNamedQuery(TestUser.FIND_ALL, TestUser.class).getResultList();
    }
}
