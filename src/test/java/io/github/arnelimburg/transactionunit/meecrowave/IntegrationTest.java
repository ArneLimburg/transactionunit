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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MeecrowaveConfig;
import org.apache.meecrowave.testing.ConfigurationInject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.github.arnelimburg.transactionunit.RollbackAfterTest;
import io.github.arnelimburg.transactionunit.dto.UserDto;

@MeecrowaveConfig
@TestInstance(PER_CLASS)
@RollbackAfterTest
public class IntegrationTest {
    @ConfigurationInject
    private Meecrowave.Builder config;

    @Test
    @DisplayName("create and read user (first)")
    public void testFirstRun() {
        Client client = ClientBuilder.newClient();

        // BEFORE
        Response responseBefore = client.target(getBaseUrl()).request().get();
        List<UserDto> usersBefore = responseBefore.readEntity(new GenericType<List<UserDto>>() { } );
        assertThat(usersBefore.size()).isEqualTo(0);

        // WHEN
        UserDto postedUser = new UserDto("John Doe");
        client.target(getBaseUrl()).request().post(Entity.entity(postedUser, MediaType.APPLICATION_JSON));

        // THEN
        Response responseAfter = client.target(getBaseUrl()).request().get();
        List<UserDto> usersAfter = responseAfter.readEntity(new GenericType<List<UserDto>>() { } );
        assertThat(usersAfter.stream().map(UserDto::getName)).containsExactly(postedUser.getName());

    }

    @Test
    @DisplayName("create and read user (second)")
    public void testSecondRun() {
        Client client = ClientBuilder.newClient();

        // BEFORE
        Response responseBefore = client.target(getBaseUrl()).request().get();
        List<UserDto> usersBefore = responseBefore.readEntity(new GenericType<List<UserDto>>() { } );
        assertThat(usersBefore.size()).isEqualTo(0);

        // WHEN
        UserDto postedUser = new UserDto("Jane Doe");
        client.target(getBaseUrl()).request().post(Entity.entity(postedUser, MediaType.APPLICATION_JSON));

        // THEN
        Response responseAfter = client.target(getBaseUrl()).request().get();
        List<UserDto> usersAfter = responseAfter.readEntity(new GenericType<List<UserDto>>() { } );
        assertThat(usersAfter.stream().map(UserDto::getName)).containsExactly(postedUser.getName());
    }

    private String getBaseUrl() {
        return "http://localhost:" + config.getHttpPort() + "/test-user";
    }
}
