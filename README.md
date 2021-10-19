[![maintained](https://img.shields.io/badge/Maintained-yes-brightgreen.svg)](https://github.com/ArneLimburg/transactionunit/graphs/commit-activity)
[![maven central](https://maven-badges.herokuapp.com/maven-central/org.transactionunit/transactionunit/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.transactionunit/transactionunit)
![build](https://github.com/ArneLimburg/transactionunit/workflows/build/badge.svg) 
[![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_transactionunit&metric=security_rating)](https://sonarcloud.io/dashboard?id=ArneLimburg_transactionunit)
[![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_transactionunit&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=ArneLimburg_transactionunit)
[![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_transactionunit&metric=bugs)](https://sonarcloud.io/dashboard?id=ArneLimburg_transactionunit)
[![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_transactionunit&metric=coverage)](https://sonarcloud.io/dashboard?id=ArneLimburg_transactionunit)
[![Liberapay](https://img.shields.io/badge/Liberapay-Donate-%23f6c915.svg)](https://liberapay.com/arnelimburg)
[![Ko-Fi](https://img.shields.io/badge/Ko--fi-Buy%20me%20a%20coffee!-%2346b798.svg)](https://ko-fi.com/arnelimburg)

# TransactionUnit

TransactionUnit is a framework that provides advanced rollback-after-test behavior. 

Database tests are one of the longest running tests in today's enterprise applications.
Database startup and setup usually take a long amount of the time.
On the other hand one normally want to test against the database technology that is used in production.
However using one database instance for the complete test suite has a major draw back.
Tests are not independent any more and may affect future tests.

TransactionUnit addresses this problem by hooking into JPA, behave like transactions behave,
but rolling back all transactions after the test and returning the database into the state before the test.
