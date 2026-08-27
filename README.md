# Product Costing + Supplier Intelligence Platform

Enterprise product costing, should-cost, and vendor selection for automotive manufacturing.

## Structure
- `backend/` — Java 21, Spring Boot, MySQL, Flyway (V90)
- `frontend/` — React, TypeScript, MUI
- `docs/` — API, schema, status, changelog
- `AI_AGENT/` — continuity files for multi-agent development

## Quick start (host)
1. MySQL 8 + Flyway migrate to V90
2. `cd backend && mvn spring-boot:run`
3. `cd frontend && npm install && npm run dev`
4. Login: see seed / demo credentials in docs

## Central Access (V116)
Module entitlement is enforced on Vendor Selection APIs via `@RequiresModule` and `X-Customer-Id`.
Platform Admin manages customers, modules, users, and roles. Frontend is not a security boundary.

See `PROJECT_STATUS.md` and `NEXT_AGENT.md`.


## Docker test stack

Full stack (MySQL + backend + frontend):

| File | Role |
|------|------|
| [`docker-compose.yml`](docker-compose.yml) | Root compose — services `mysql`, `backend`, `frontend` |
| [`backend/Dockerfile`](backend/Dockerfile) | Multi-stage Temurin 21 Spring Boot image |
| [`frontend/Dockerfile`](frontend/Dockerfile) | Multi-stage Node build + nginx SPA |
| [`frontend/nginx.conf`](frontend/nginx.conf) | SPA + `/api` reverse proxy to `backend:8080` |
| [`backend/docker-compose.yml`](backend/docker-compose.yml) | Backend + MySQL only (optional) |
| [`scripts/v230-host-gate.sh`](scripts/v230-host-gate.sh) | One-shot Docker + Playwright gate |

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
docker compose up --build -d
# Frontend http://localhost:8088  ·  Backend http://localhost:8080  ·  MySQL localhost:3307
curl -s http://127.0.0.1:8080/actuator/health
```

See [`docs/DOCKER_TEST_STACK.md`](docs/DOCKER_TEST_STACK.md).

## Operations

- [Production Deployment Runbook](docs/PRODUCTION_DEPLOYMENT_RUNBOOK.md)
- [Production Security Checklist](docs/PRODUCTION_SECURITY_CHECKLIST.md)
- [Docker IT Status](docs/DOCKER_IT_STATUS.md)
- [Full-scale QA Report](docs/FULL_SCALE_QA_REPORT.md)

## Verification

```bash
# Unit + Vitest (Flyway IT if TEST_DB_URL set)
./scripts/ci-verify.sh

## CI/CD

| Tool | Path |
|------|------|
| GitHub Actions | `.github/workflows/ci.yml`, `release-gate.yml` |
| GitLab CI | `.gitlab-ci.yml` |
| Local verify | `./scripts/ci-verify.sh` |
| Documentation | `docs/CI_CD_PIPELINE.md` |



# Post-deploy smoke
BASE=http://127.0.0.1:8080 ./scripts/prod-smoke.sh
```

## Production gate
- [Production Dry-Run Checklist](docs/PRODUCTION_DRY_RUN_CHECKLIST.md)
- [Production Readiness Gate](docs/PRODUCTION_READINESS_GATE.md)
