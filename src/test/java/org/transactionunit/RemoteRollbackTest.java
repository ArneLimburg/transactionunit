/*
 * Copyright 2026 Arne Limburg
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class RemoteRollbackTest {

    private static final int HTTP_STATUS_NO_CONTENT = 204;
    private static final int HTTP_STATUS_INTERNAL_ERROR = 500;

    private HttpServer server;
    private AtomicInteger hitCount;

    @BeforeEach
    void startServer() throws IOException {
        hitCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/transactions", exchange -> {
            hitCount.incrementAndGet();
            exchange.sendResponseHeaders(HTTP_STATUS_NO_CONTENT, -1);
            exchange.close();
        });
        server.createContext("/transactions/error", exchange -> {
            hitCount.incrementAndGet();
            exchange.sendResponseHeaders(HTTP_STATUS_INTERNAL_ERROR, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Remote endpoint receives DELETE on rollbackRemote")
    void remoteEndpointReceivesRollback() throws Exception {
        TransactionUnitExtension.rollbackRemote(endpoint("/transactions"));
        assertThat(hitCount).hasValue(1);
    }

    @Test
    @DisplayName("Non-2xx responses cause rollbackRemote to fail")
    void nonSuccessResponseCausesFailure() {
        URI uri = endpoint("/transactions/error");
        assertThatThrownBy(() -> TransactionUnitExtension.rollbackRemote(uri))
            .isInstanceOf(IOException.class);
        assertThat(hitCount).hasValue(1);
    }

    @Test
    @DisplayName("Unreachable endpoint causes rollbackRemote to fail")
    void unreachableEndpointCausesFailure() {
        URI uri = URI.create("http://localhost:1/transactions");
        assertThatThrownBy(() -> TransactionUnitExtension.rollbackRemote(uri))
            .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Interrupted thread propagates InterruptedException")
    void interruptedThreadPropagatesException() {
        try {
            Thread.currentThread().interrupt();
            assertThatThrownBy(() -> TransactionUnitExtension.rollbackRemote(endpoint("/transactions")))
                .isInstanceOf(InterruptedException.class);
        } finally {
            Thread.interrupted();
        }
    }

    private URI endpoint(String path) {
        return URI.create("http://localhost:" + server.getAddress().getPort() + path);
    }
}
