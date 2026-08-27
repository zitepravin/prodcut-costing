# Product Costing Software — Backend

Java 21 / Spring Boot 3.3 backend for the enterprise Product Costing Software (Automobile Industry).

**Governance docs (read first):** `docs/PROJECT_STATUS.md`, `docs/CURRENT_CONTEXT.md`, `docs/API_SPEC.md`, `docs/DATABASE_SCHEMA.md`, `docs/NEXT_AGENT.md`, `docs/TASKS.md`, `docs/CHANGELOG.md`.

## Status (2026-08-12)

Phase 1 core is substantially present in code (incl. M12 Currency Management):

- Sprint 0 foundation (JWT, RBAC, MFA, audit, OpenAPI)
- M01 Master Data Management
- M02 BOM & Product Structure + BomRollupEngine
- M03 Raw Material Costing (index-linked + commodity index)
- M04 Process & Labor Costing
- Tooling Costing
- Should-Cost Engine + benchmarks
- Approval Workflow + escalation
- Notifications

**This code has not yet been compiled or run in the current sandbox** — run `mvn clean test` on a machine with network + JDK 21 + MySQL 8.

See `docs/PROJECT_STATUS.md` for the full module matrix and known gaps.

## Run locally

1. Create a MySQL 8 database `product_costing` and a user with DML + Flyway migration privileges (see `V1__platform_and_audit_baseline.sql` for the audit_log grant notes).
2. Set environment variables (or edit `application.yml` defaults):
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
   - `JWT_SECRET` (256-bit minimum, do not use the placeholder in production)
3. `mvn spring-boot:run` (or `./scripts/build.sh`)
4. Swagger UI: `http://localhost:8080/swagger-ui.html`

## Package structure

```
com.automotive.costing
├── config/          Security, JWT, OpenAPI, JPA auditor
├── security/        JWT token provider + filter
├── common/          Base entity, audit, exceptions, events, DTOs
├── platform/user/   Auth, Admin, MFA, AccessScope
├── masterdata/      M01
├── bom/             M02 + BomRollupEngine
├── material/        M03
├── process/         M04
├── tooling/         Tooling records + cost engine
├── shouldcost/      M08
├── workflow/        Approval workflow engine
└── notification/    In-app (+ optional email)
```

Each module follows `entity / dto / (mapper) / repository / service / controller / engine`.

## What's next

See `docs/NEXT_AGENT.md` and `docs/TASKS.md`. Highest priority: verify build, harden leaf cost resolution, expand frontend parity, then Currency Management or Packaging & Logistics.
