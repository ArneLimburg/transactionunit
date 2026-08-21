/*
 * Copyright 2021 Olaf Prins, Arne Limburg
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
package org.transactionunit.spring;

import static java.util.Collections.synchronizedMap;

import java.util.IdentityHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.transactionunit.TransactionUnitEntityManagerFactory;

import jakarta.persistence.EntityManagerFactory;

public class EntityManagerFactoryBeanPostProcessor implements BeanPostProcessor {

    /**
     * Spring may run the post processor repeatedly for the same {@link EntityManagerFactory} bean,
     * so the wrapper has to be cached per delegate. Creating a new wrapper per invocation would hand
     * out a separate entity manager - and thus a separate persistence context - to every consumer,
     * so that entities persisted via one of them are never flushed by another.
     */
    private final Map<EntityManagerFactory, TransactionUnitEntityManagerFactory> entityManagerFactories
        = synchronizedMap(new IdentityHashMap<>());
    private TransactionUnitEntityManagerFactory entityManagerFactory;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof TransactionUnitEntityManagerFactory alreadyWrapped) {
            return alreadyWrapped;
        }
        if (bean instanceof EntityManagerFactory emf) {
            entityManagerFactory = entityManagerFactories.computeIfAbsent(emf, TransactionUnitEntityManagerFactory::new);
            return entityManagerFactory;
        }
        if (entityManagerFactory != null && entityManagerFactory.isRollbackOnly()) {
            entityManagerFactory.rollbackAll();
        }
        return bean;
    }
}
