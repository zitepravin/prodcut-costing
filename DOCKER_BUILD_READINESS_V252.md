# Docker image build readiness — V252

## Offline verification (agent)

| Check | Result |
|-------|--------|
| Backend `mvn -DskipTests package` | **BUILD SUCCESS** |
| Frontend `npm run typecheck` | **PASS** (after FE fix) |
| Frontend `npm run build` | **PASS** (vite production) |
| Docker daemon on agent | **Not available** — host must run `docker compose build` |

## Fix applied this review

**Frontend TypeScript (would fail Docker FE stage `npm run build`):**
- `PurchaseRequestsPage.tsx`: `setPoSupplierId` / `onCreatePoFromPr` were unused under `noUnusedLocals`
- Wired **Supplier ID for PO** + **Create PO from PR** buttons so symbols are used

## Reviewed dependencies

| Area | Status |
|------|--------|
| `backend/Dockerfile` (maven:3.9.9-eclipse-temurin-21) | OK |
| `frontend/Dockerfile` (node:20-alpine + nginx) | OK — needs package-lock + .npmrc |
| `docker-compose.yml` Flyway note | Updated → V129 |
| `nginx.conf` `/api/` → `backend:8080` | OK |
| App routes + `PATH_REQUIRED_MODULE` PURCHASE | OK |
| AppLayout nav filter via `canAccessPath` | OK |
| Purchase FE API exports | OK |

## Host command

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
docker compose build
docker compose up -d
./scripts/wave-h-host-gate.sh
```
