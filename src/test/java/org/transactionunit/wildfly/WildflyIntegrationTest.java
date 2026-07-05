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
package org.transactionunit.wildfly;

import static java.net.URI.create;
import static java.net.http.HttpRequest.newBuilder;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.net.http.HttpResponse.BodyHandlers.discarding;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.transactionunit.RollbackAfterTest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Testcontainers
@RollbackAfterTest(remoteUrlProperty = WildflyIntegrationTest.ROLLBACK_URL_PROPERTY)
public class WildflyIntegrationTest {

    static final String ROLLBACK_URL_PROPERTY = "transactionunit.wildfly.rollback.url";

    private static final int WILDFLY_HTTP_PORT = 8080;
    private static final int HTTP_STATUS_OK = 200;

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Network NETWORK = Network.newNetwork();

    private static final ImageFromDockerfile WILDFLY_IMAGE
        = new ImageFromDockerfile("transactionunit-wildfly-test", false)
        .withFileFromPath("pom.xml", Paths.get("pom.xml"))
        .withFileFromPath("src/main", Paths.get("src/main"))
        .withFileFromPath("src/test/wildfly/src", Paths.get("src/test/wildfly/src"))
        .withFileFromPath("target/test-classes/wildfly-pom.xml", Paths.get("target/test-classes/wildfly-pom.xml"))
        .withFileFromPath("target/test-classes/Dockerfile.wildfly", Paths.get("target/test-classes/Dockerfile.wildfly"))
        .withBuildImageCmdModifier(cmd -> cmd.withDockerfilePath("target/test-classes/Dockerfile.wildfly"));

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withNetwork(NETWORK)
        .withNetworkAliases("db")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Container
    private static final GenericContainer<?> WILDFLY = new GenericContainer<>(WILDFLY_IMAGE)
        .withNetwork(NETWORK)
        .withExposedPorts(WILDFLY_HTTP_PORT)
        .waitingFor(Wait.forHttp("/test-user").forStatusCode(HTTP_STATUS_OK).withStartupTimeout(Duration.ofMinutes(3)))
        .dependsOn(POSTGRES);

    @BeforeAll
    static void setRollbackUrl() {
        System.setProperty(ROLLBACK_URL_PROPERTY,
            "http://localhost:" + WILDFLY.getMappedPort(WILDFLY_HTTP_PORT) + "/transactions");
    }

    @AfterAll
    static void clearRollbackUrl() {
        System.clearProperty(ROLLBACK_URL_PROPERTY);
    }

    @Test
    @DisplayName("create and read user (first)")
    void createAndReadUserFirst() throws Exception {
        postUser("Joe Doe");

        List<Map<String, String>> users = getUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).get("name")).isEqualTo("Joe Doe");
    }

    @Test
    @DisplayName("create and read user (second)")
    void createAndReadUserSecond() throws Exception {
        postUser("Jane Doe");

        List<Map<String, String>> users = getUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).get("name")).isEqualTo("Jane Doe");
    }

    private void postUser(String name) throws Exception {
        HttpRequest request = newBuilder(create(baseUrl()))
            .header("Content-Type", "application/json")
            .POST(ofString(JSON.writeValueAsString(Map.of("name", name))))
            .build();
        HttpClient.newHttpClient().send(request, discarding());
    }

    private List<Map<String, String>> getUsers() throws Exception {
        HttpRequest request = newBuilder(create(baseUrl()))
            .header("Accept", "application/json")
            .GET()
            .build();
        String body = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
        return JSON.readValue(body, new TypeReference<List<Map<String, String>>>() { });
    }

    private String baseUrl() {
        return "http://localhost:" + WILDFLY.getMappedPort(WILDFLY_HTTP_PORT) + "/test-user";
    }
}
