# syntax=docker/dockerfile:1.7
# Multi-stage: Maven + JDK 21 → JRE 21 runtime
# Cache strategy:
#   1) pom.xml only → dependency resolve (stable layer)
#   2) src/main only → package (tests excluded from compile context)
#   3) BuildKit cache mount for ~/.m2 across builds

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Layer 1: dependency metadata (invalidates only when pom changes)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests dependency:go-offline dependency:resolve-plugins

# Layer 2: production sources only (test source changes do not bust this layer)
COPY src/main ./src/main
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package \
    && cp target/product-costing-backend-*.jar /app/app.jar

# Runtime — small JRE image; curl for compose healthcheck
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update -qq \
    && apt-get install -y -qq --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
