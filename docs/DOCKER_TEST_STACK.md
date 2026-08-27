# Docker test stack

## Layout
- Root `docker-compose.yml` — MySQL 8 + Spring Boot backend + React (nginx) frontend
- `backend/Dockerfile` — multi-stage Temurin 21
- `frontend/Dockerfile` + `nginx.conf` — SPA + `/api` proxy to backend


## Compose build references

Root `docker-compose.yml` points at:

- `backend/Dockerfile` via `build.context: ./backend` + `dockerfile: Dockerfile`
- `frontend/Dockerfile` via `build.context: ./frontend` + `dockerfile: Dockerfile`

Backend-only compose (`backend/docker-compose.yml`) uses `context: .` + `dockerfile: Dockerfile` (i.e. `backend/Dockerfile`).

## Flyway
Migrations **V1 → V91** apply automatically when the backend starts (`spring.flyway.enabled=true`).  
Do not invent migrations. Application package version ≠ Flyway version.

## Start (Windows / Linux with working Docker)

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
docker compose up --build -d
# Frontend http://localhost:8088
# Backend  http://localhost:8080
# MySQL    localhost:3307
```

Health:
```bash
curl -s http://localhost:8080/actuator/health
./scripts/prod-smoke.sh   # BASE=http://localhost:8080
```

## Tests inside / against stack

```bash
# Unit (host JDK 21)
cd backend && mvn test

# Host Flyway IT against compose MySQL
export TEST_DB_URL='jdbc:mysql://127.0.0.1:3307/product_costing?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export TEST_DB_USERNAME=costing_app TEST_DB_PASSWORD=changeme
mvn -Dtest=FlywayMigrationIntegrationTest test

# Frontend
cd frontend && npm ci && npx vitest run
npx playwright test
```

## Notes
- Port **3307** host → 3306 container (avoids clash with local MySQL)
- Volume `pcs_mysql_data` persists test DB
- Entitlements activate only after verified payment webhook (not FE success)
