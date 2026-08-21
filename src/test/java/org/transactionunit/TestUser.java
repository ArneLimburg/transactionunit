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

import static org.transactionunit.TestUser.FIND_ALL;
import static org.transactionunit.TestUser.WITH_NAME;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedQuery;

@Entity
// the result class is needed for EntityManagerFactory#getNamedQueries(Class) to find this query
@NamedQuery(name = FIND_ALL, query = "SELECT u FROM TestUser u", resultClass = TestUser.class)
@NamedEntityGraph(name = WITH_NAME, attributeNodes = @NamedAttributeNode("name"))
public class TestUser {

    public static final String FIND_ALL = "TestUser.findAll";
    public static final String WITH_NAME = "TestUser.withName";

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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
