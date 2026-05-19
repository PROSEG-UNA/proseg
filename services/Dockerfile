FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

ARG SERVICE_NAME

COPY pom.xml .
COPY msvc-common ./msvc-common
COPY msvc-config ./msvc-config
COPY msvc-eureka ./msvc-eureka
COPY msvc-gateway ./msvc-gateway
COPY msvc-auth ./msvc-auth
COPY msvc-email ./msvc-email
COPY msvc-inventory ./msvc-inventory
COPY msvc-archive ./msvc-archive
COPY msvc-transport ./msvc-transport
COPY msvc-document-processor ./msvc-document-processor

RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

ARG SERVICE_NAME

COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]