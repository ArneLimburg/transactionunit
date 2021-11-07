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

import static java.util.Optional.ofNullable;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.transactionunit.TransactionUnitProvider.getInstance;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.transactionunit.RollbackAfterTest.Type;

public class TransactionUnitExtension implements AfterAllCallback, AfterEachCallback, AfterTestExecutionCallback {

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        if (getType(context) == Type.EXECUTION) {
            getInstance().rollbackAll();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (getType(context) == Type.METHOD) {
            getInstance().rollbackAll();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (getType(context) == Type.CLASS) {
            getInstance().rollbackAll();
        }
    }

    private Type getType(ExtensionContext context) {
        return ofNullable(
                findAnnotation(context.getTestMethod(), RollbackAfterTest.class)
                .orElse(findAnnotation(context.getTestClass(), RollbackAfterTest.class)
                .orElse(null))).map(RollbackAfterTest::value)
        .orElse(Type.METHOD);
    }
}
