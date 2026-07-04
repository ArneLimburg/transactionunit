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
package org.transactionunit.meecrowave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.transactionunit.TransactionUnitProvider.PERSISTENCE_PROVIDER_PROPERTY;

import java.util.List;

import org.apache.meecrowave.junit5.MeecrowaveConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.transactionunit.RollbackAfterTest;

import jakarta.enterprise.context.Dependent;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceProperty;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Dependent
@MeecrowaveConfig(httpPort = RemoteRollbackMeecrowaveIntegrationTest.HTTP_PORT)
@TestInstance(PER_CLASS)
@RollbackAfterTest(remoteUrl = "http://localhost:" + RemoteRollbackMeecrowaveIntegrationTest.HTTP_PORT + "/transactions")
@PersistenceContext(unitName = "test-unit", properties = {
    @PersistenceProperty(name = "jakarta.persistence.provider", value = "io.github.arnelimburg.transactionunit.TransactionUnitProvider"),
    @PersistenceProperty(name = PERSISTENCE_PROVIDER_PROPERTY, value = "org.hibernate.jpa.HibernatePersistenceProvider")} )
public class RemoteRollbackMeecrowaveIntegrationTest {

    static final int HTTP_PORT = 18080;

    private static final int HTTP_STATUS_NO_CONTENT = 204;

    @Test
    @DisplayName("first test creates a user")
    public void createsUser() {
        Client client = ClientBuilder.newClient();

        Response responseBefore = client.target(baseUrl()).request().get();
        List<UserDto> usersBefore = responseBefore.readEntity(new GenericType<List<UserDto>>() { } );
        assertThat(usersBefore).isEmpty();

        UserDto postedUser = new UserDto("John Doe");
        client.target(baseUrl()).request().post(Entity.entity(postedUser, MediaType.APPLICATION_JSON));

        Response responseAfter = client.target(baseUrl()).request().get();
        List<UserDto> usersAfter = responseAfter.readEntity(new GenericType<List<UserDto>>() { } );
        assertThat(usersAfter).extracting(UserDto::getName).containsExactly(postedUser.getName());
    }

    @Test
    @DisplayName("second test sees empty state (remote rollback via RollbackFilter reset the previous test)")
    public void previousStateHasBeenRolledBack() {
        Client client = ClientBuilder.newClient();

        Response response = client.target(baseUrl()).request().get();
        List<UserDto> users = response.readEntity(new GenericType<List<UserDto>>() { } );
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("non-DELETE requests to /transactions are passed through the filter chain")
    public void nonDeleteRequestPassesFilter() {
        Client client = ClientBuilder.newClient();

        Response response = client.target("http://localhost:" + HTTP_PORT + "/transactions").request().get();
        assertThat(response.getStatus()).isNotEqualTo(HTTP_STATUS_NO_CONTENT);
    }

    private String baseUrl() {
        return "http://localhost:" + HTTP_PORT + "/test-user";
    }
}
