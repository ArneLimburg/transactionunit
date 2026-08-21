/*
 * Copyright 2021 Tim Wuellner
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * This test runs in a forked jvm of its own, where no persistence provider has been created yet. The order of the
 * test methods matters, because {@link #providerIsResolvedFromClassPath()} needs a jvm where
 * {@link TransactionUnitProvider} has not been instantiated yet and the other test instantiates one.
 */
@Tag("without-hibernate")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NoPersistenceProviderInitializedTest {

    @Test
    @Order(1)
    @DisplayName("The provider is resolved from the class path when none has been created yet")
    public void providerIsResolvedFromClassPath() {
        assertThat(TransactionUnitProvider.getInstance()).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("No ProviderUtil is available when no delegate is initialized")
    public void noProviderUtilWithoutDelegate() {
        assertThrows(IllegalStateException.class, () -> new TransactionUnitProvider().getProviderUtil());
    }
}
