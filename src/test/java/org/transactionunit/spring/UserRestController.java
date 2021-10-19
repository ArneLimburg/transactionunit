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
package org.transactionunit.spring;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.transactionunit.TestUser;

/**
 * @author Olaf Prins - open knowledge GmbH
 */
@RestController
@Transactional
public class UserRestController {

    private final TestUserJpaRepository testUserJpaRepository;

    public UserRestController(TestUserJpaRepository testUserJpaRepository) {
        this.testUserJpaRepository = testUserJpaRepository;
    }

    @GetMapping("/users")
    public List<TestUser> users() {
        return testUserJpaRepository.findAll();
    }

    @PostMapping("/users")
    public void createCustomer(@RequestBody TestUser user) {
        testUserJpaRepository.save(user);
    }
}
