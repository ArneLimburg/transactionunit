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
package org.transactionunit.meecrowave;

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.List;

import org.transactionunit.TestUser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
@Path("/test-user")
@Transactional
public class TestResource {

    @Inject
    TestRepository testRepository;

    @POST
    public Response addUser(@Context UriInfo uriInfo, UserDto userDto) {
        TestUser user = testRepository.persistUser(new TestUser(userDto.getName()));

        String uri = String.format(uriInfo.getBaseUri() + "/%s", user.getId());
        return Response.created(URI.create(uri)).build();
    }

    @GET
    public List<UserDto> getUsers() {
        return testRepository.getAllUsers().stream().map(TestUser::getName).map(UserDto::new).collect(toList());
    }
}
