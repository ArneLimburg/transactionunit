# syntax=docker/dockerfile:1
ARG TU_VERSION=1.1.0-SNAPSHOT

FROM maven:3.9-eclipse-temurin-17 AS war-build
ARG TU_VERSION
WORKDIR /war
COPY src/test/wildfly/pom.xml pom.xml
COPY src/test/wildfly/src ./src
COPY target/transactionunit-${TU_VERSION}.jar /tmp/transactionunit.jar
COPY target/transactionunit-${TU_VERSION}-web.jar /tmp/transactionunit-web.jar
RUN mvn -B -q install:install-file \
      -Dfile=/tmp/transactionunit.jar \
      -DgroupId=org.transactionunit \
      -DartifactId=transactionunit \
      -Dversion=${TU_VERSION} \
      -Dpackaging=jar \
 && mvn -B -q install:install-file \
      -Dfile=/tmp/transactionunit-web.jar \
      -DgroupId=org.transactionunit \
      -DartifactId=transactionunit \
      -Dversion=${TU_VERSION} \
      -Dpackaging=jar \
      -Dclassifier=web \
 && mvn -B -q -Ptransactionunit package

FROM quay.io/wildfly/wildfly:36.0.1.Final-jdk17
ENV ORG_TRANSACTIONUNIT_PERSISTENCE_PROVIDER="org.hibernate.jpa.HibernatePersistenceProvider"
COPY --from=war-build /war/target/ROOT.war /opt/jboss/wildfly/standalone/deployments/
