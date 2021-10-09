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
package io.github.arnelimburg.transactionunit;

import static io.github.arnelimburg.transactionunit.TestUser.FIND_ALL;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;

@Entity
@NamedQuery(name = FIND_ALL, query = "SELECT u FROM TestUser u")
public class TestUser {

    public static final String FIND_ALL = "TestUser.findAll";

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    protected TestUser() {
        // for jpa
    }

    public TestUser(String name) {
        this.name = name;
    }
}
