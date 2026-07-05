# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-17 AS war-build
WORKDIR /war
COPY src/test/wildfly/pom.xml pom.xml
COPY src/test/wildfly/src ./src
RUN mvn -B -q -Ptransactionunit package

FROM quay.io/wildfly/wildfly:36.0.1.Final-jdk17
ENV ORG_TRANSACTIONUNIT_PERSISTENCE_PROVIDER="org.hibernate.jpa.HibernatePersistenceProvider"
COPY --from=war-build /war/target/ROOT.war /opt/jboss/wildfly/standalone/deployments/
