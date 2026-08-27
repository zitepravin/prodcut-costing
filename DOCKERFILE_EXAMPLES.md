# Dockerfile examples — backend & frontend

These are the production Dockerfiles shipped in the master package (`backend/Dockerfile`, `frontend/Dockerfile`).

Requires **BuildKit**:
```bash
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
```

---

## Backend (`backend/Dockerfile`)

```dockerfile
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
```

**Build alone:**
```bash
cd backend
docker build -t pcs-backend:local .
```

---

## Frontend (`frontend/Dockerfile`)

```dockerfile
# syntax=docker/dockerfile:1.7
# Multi-stage: Node 20 build → nginx static
# Cache strategy:
#   1) package.json + lock + .npmrc → npm ci (stable)
#   2) app sources only → vite build
#   3) BuildKit cache mount for npm

FROM node:20-alpine AS build
WORKDIR /app

# Layer 1: dependency install (invalidates only when lock/package changes)
COPY package.json package-lock.json .npmrc ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --prefer-offline --no-audit --no-fund

# Layer 2: sources required for production build only
COPY index.html vite.config.ts tsconfig.json tsconfig.app.json tsconfig.node.json ./
COPY public ./public
COPY src ./src
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

**Build alone:**
```bash
cd frontend
docker build -t pcs-frontend:local --build-arg VITE_API_BASE_URL= .
```

Empty `VITE_API_BASE_URL` = relative `/api` (nginx proxies to `backend:8080` in compose).

---

## Compose (both services)

```bash
export DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1
export JWT_SECRET="$(openssl rand -base64 48)"
docker compose build --parallel
docker compose up -d
```
