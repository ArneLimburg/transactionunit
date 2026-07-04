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

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.transactionunit.TransactionUnitProvider.getInstance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.transactionunit.RollbackAfterTest.Type;

public class TransactionUnitExtension implements AfterAllCallback, AfterEachCallback, AfterTestExecutionCallback {

    private static final Duration REMOTE_ROLLBACK_TIMEOUT = Duration.ofSeconds(10);
    private static final int HTTP_ERROR_STATUS = 400;

    @Override
    public void afterTestExecution(ExtensionContext context) throws IOException, InterruptedException {
        rollbackIfMatches(context, Type.EXECUTION);
    }

    @Override
    public void afterEach(ExtensionContext context) throws IOException, InterruptedException {
        rollbackIfMatches(context, Type.METHOD);
    }

    @Override
    public void afterAll(ExtensionContext context) throws IOException, InterruptedException {
        rollbackIfMatches(context, Type.CLASS);
    }

    private void rollbackIfMatches(ExtensionContext context, Type expected) throws IOException, InterruptedException {
        Optional<RollbackAfterTest> annotation = findRollbackAnnotation(context);
        Type type = annotation.map(RollbackAfterTest::value).orElse(Type.METHOD);
        if (type != expected) {
            return;
        }
        getInstance().rollbackAll();
        Optional<URI> remoteUrl = annotation.flatMap(TransactionUnitExtension::resolveRemoteUrl);
        if (remoteUrl.isPresent()) {
            rollbackRemote(remoteUrl.get());
        }
    }

    private Optional<RollbackAfterTest> findRollbackAnnotation(ExtensionContext context) {
        return findAnnotation(context.getTestMethod(), RollbackAfterTest.class)
            .or(() -> findAnnotation(context.getTestClass(), RollbackAfterTest.class));
    }

    private static Optional<URI> resolveRemoteUrl(RollbackAfterTest annotation) {
        String url = annotation.remoteUrl();
        if (url.isEmpty() && !annotation.remoteUrlProperty().isEmpty()) {
            url = System.getProperty(annotation.remoteUrlProperty(), "");
        }
        return url.isEmpty() ? Optional.empty() : Optional.of(URI.create(url));
    }

    static void rollbackRemote(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(REMOTE_ROLLBACK_TIMEOUT)
            .DELETE()
            .build();
        HttpResponse<Void> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() >= HTTP_ERROR_STATUS) {
            throw new IOException("Remote rollback at " + uri + " returned HTTP " + response.statusCode());
        }
    }
}
