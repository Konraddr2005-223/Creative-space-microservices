# Multi-module Boot fat-jar image. Build context = repo root.
# Usage: docker build --build-arg MODULE=booking-service -t cs-booking-service .
#
# WHY Maven image (not mvnw): wrapper JAR is gitignored; CI/Docker need a reliable toolchain.

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY observability observability
COPY service-discovery service-discovery
COPY api-gateway api-gateway
COPY resource-service resource-service
COPY booking-service booking-service
COPY payment-service payment-service
COPY notification-service notification-service
ARG MODULE
RUN mvn -pl "${MODULE}" -am -DskipTests package \
    && JAR="$(ls "${MODULE}"/target/"${MODULE}"-*.jar | grep -v '\.original$' | head -n1)" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
