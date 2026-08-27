# Docker Compose Networking Investigation

**Date:** 2026-08-14  
**Package:** MASTER V153  
**Scope:** local `docker-compose.yml` for Product Costing stack

## Topology (intended)

```
Browser (host)
    │
    ├─ http://localhost:8088  ──►  pcs-frontend (nginx:80)
    │                                   │
    │                                   ├─ static SPA
    │                                   └─ /api/*, /actuator/*  ──proxy──►  pcs-backend:8080
    │
    └─ http://localhost:8080  ──►  pcs-backend (optional direct)
                                          │
                                          └─ JDBC ──►  pcs-mysql:3306
                                                         │
Host tools ── localhost:3307 ─────────────────────────────┘
```

**Internal network:** `pcs-net` (bridge). DNS names: `mysql`, `backend`, `frontend`.

## Port map

| Host | Container | Service |
|------|-----------|---------|
| 8088 | 80 | frontend nginx |
| 8080 | 8080 | Spring Boot |
| 3307 | 3306 | MySQL 8 |

## Findings

### F1 — CRITICAL (fixed V153): Vite env name mismatch

| Item | Before | After |
|------|--------|-------|
| Dockerfile set | `ENV VITE_API_BASE=` | `ARG/ENV VITE_API_BASE_URL=` |
| App reads | `import.meta.env.VITE_API_BASE_URL` | same |

**Effect before fix:** Docker build never set `VITE_API_BASE_URL`, so the SPA defaulted to `http://localhost:8080` and **bypassed nginx**. Worked only when:

- Browser was on the same host, and  
- CORS allowed `http://localhost:8088`

Failed for: remote host access, hostname ≠ localhost, or backend port not published.

**Fix:** Build with empty `VITE_API_BASE_URL` so axios uses relative `/api/...` → nginx → `http://backend:8080`.

### F2 — CORRECT: Backend → MySQL host

`DB_URL=jdbc:mysql://mysql:3306/...` uses Compose DNS.  
Using `localhost` inside the backend container would point at the backend itself — **wrong**.

### F3 — CORRECT: Healthchecks

- MySQL: `mysqladmin ping -h 127.0.0.1` (inside mysql container)  
- Backend: `curl http://127.0.0.1:8080/actuator/health` (inside backend container)

### F4 — CORRECT: nginx upstream

`proxy_pass http://backend:8080` resolves on `pcs-net`.

### F5 — CORS

- Compose sets `CORS_ORIGINS` for direct browser→:8080 calls.  
- Preferred path after F1 fix: same-origin via :8088 → **no CORS** for API.  
- `application-local.yml` also allows `*` patterns for local profile.

### F6 — Dev without Docker

Vite had **no** proxy; `npm run dev` on :5173 called :8080 and relied on CORS.  
**Fixed V153:** Vite proxies `/api` and `/actuator` to `127.0.0.1:8080`.

### F7 — No explicit network (before V153)

Compose default bridge still DNS-resolves service names. Explicit `pcs-net` added for clarity and isolation from other projects.

## Common failures & checks

| Symptom | Likely cause | Check |
|---------|--------------|-------|
| Backend cannot start / JDBC refused | DB host `localhost` in container | `DB_URL` must use host `mysql` |
| FE login fails CORS | FE calling :8080 with wrong origin | Rebuild FE with empty `VITE_API_BASE_URL`; use :8088 |
| FE 502 on /api | Backend not healthy / wrong upstream | `docker compose ps`; nginx `backend:8080` |
| MySQL port conflict | Host 3307 in use | Change host mapping e.g. `3308:3306` |
| Overlay/whiteout on some agents | Host Docker limitation | Not app networking; use customer Docker Desktop |

## Verify on customer host

```bash
docker compose up --build -d
docker compose ps
docker network inspect pcs-net
curl -s http://localhost:8080/actuator/health
curl -s -o /dev/null -w "%{http_code}" http://localhost:8088/
# Same-origin API via nginx (should be 401/403 without token, not 502)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8088/api/v1/auth/login
```

From browser DevTools on `http://localhost:8088`: API calls should show as `http://localhost:8088/api/...` not `http://localhost:8080/api/...`.
