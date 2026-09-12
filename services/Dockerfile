FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

ARG SERVICE_NAME
ARG EXTRA_MODULE=msvc-common

COPY pom.xml .
COPY msvc-common/pom.xml ./msvc-common/
COPY msvc-config/pom.xml ./msvc-config/
COPY msvc-gateway/pom.xml ./msvc-gateway/
COPY msvc-auth/pom.xml ./msvc-auth/
COPY msvc-email/pom.xml ./msvc-email/
COPY msvc-inventory/pom.xml ./msvc-inventory/
COPY msvc-archive/pom.xml ./msvc-archive/
COPY msvc-transport/pom.xml ./msvc-transport/
COPY msvc-document-processor/pom.xml ./msvc-document-processor/
COPY msvc-maintenance/pom.xml ./msvc-maintenance/

RUN mvn -B -pl ${SERVICE_NAME} -am -DskipTests \
      -Dmaven.repo.local=/app/.m2 dependency:go-offline || true

COPY msvc-common/src ./msvc-common/src
COPY ${EXTRA_MODULE}/src ./${EXTRA_MODULE}/src
COPY ${SERVICE_NAME}/src ./${SERVICE_NAME}/src

RUN mvn -B -pl ${SERVICE_NAME} -am \
      -Dmaven.repo.local=/app/.m2 clean verify

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

ARG SERVICE_NAME

COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
