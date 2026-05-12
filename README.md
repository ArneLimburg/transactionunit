[![maintained](https://img.shields.io/badge/Maintained-yes-brightgreen.svg)](https://github.com/ArneLimburg/transactionunit/graphs/commit-activity)
[![Maven Central Version](https://img.shields.io/maven-central/v/org.transactionunit/transactionunit)](https://central.sonatype.com/artifact/org.transactionunit/transactionunit)
![Maven Central Version](https://img.shields.io/maven-central/v/org.transactionunit/transactionunit)
![build](https://github.com/ArneLimburg/transactionunit/workflows/build/badge.svg)
[![Method Coverage](https://img.shields.io/badge/method%20coverage-100%25-brightgreen)](https://github.com/ArneLimburg/transactionunit/blob/main/pom.xml#L479)
[![Branch Coverage](https://img.shields.io/badge/branch%20coverage-95%25-brightgreen)](https://github.com/ArneLimburg/transactionunit/blob/main/pom.xml#L495)
[![Liberapay](https://img.shields.io/badge/Liberapay-Donate-%23f6c915.svg)](https://liberapay.com/arnelimburg)
[![Ko-Fi](https://img.shields.io/badge/Ko--fi-Buy%20me%20a%20coffee!-%2346b798.svg)](https://ko-fi.com/arnelimburg)

# TransactionUnit

TransactionUnit is a JUnit-5-Extension that provides advanced rollback-after-test behavior. 

## Rolling back the transaction after test

Database tests are one of the longest running tests in today's enterprise applications.
Database startup and setup usually take a long amount of the time.
On the other hand one normally wants to test against the database technology that is used in production.
However using one database instance for the complete test suite has a major draw back.
Tests are not independent any more and may affect future tests.

TransactionUnit addresses this problem by hooking into JPA, behave like transactions behave,
but rolling back all transactions after the test.
