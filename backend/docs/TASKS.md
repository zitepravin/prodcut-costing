# TASKS.md — Phase 1 backlog (prioritized)

## P0 — Correctness & operability
- [x] Successful `mvn clean test` on JDK 21 + MySQL 8 — 2026-08-12 (129 tests green)
- [x] Successful frontend `npm run build` — 2026-08-12
- [x] docker-compose.yml (MySQL + backend) for local demo — 2026-08-12
- [x] Complete leaf unit-cost composition in BomNodeService (material + process + tooling + scrap%) and event-driven program re-roll-up — 2026-08-12

## P1 — Frontend parity
- [x] Process Costing screens (routing + cost calc) — 2026-08-12
- [x] Tooling screens (asset + amortization) — 2026-08-12
- [x] Should-Cost detail + approval submit from UI — 2026-08-12
- [x] Approvals inbox decision actions fully wired — 2026-08-12 (entity context on inbox steps)
- [x] Materials page: version history + bulk import UI — 2026-08-12

## P2 — Missing modules (backend first)
- [x] Currency Management — 2026-08-12
- [x] Wire CurrencyService into multi-currency BOM / should-cost roll-ups — 2026-08-12
- [x] Packaging & Logistics costing (backend) — 2026-08-12
- [x] Target Costing (target vs should-cost variance, workflow integration) — 2026-08-12
- [x] RFQ Management + Supplier Costing (**backend** V16; frontend pending)
- [x] Cost Comparison workspace — 2026-08-13
- [x] Import/Export expansion — 2026-08-13
- [x] Dashboard KPIs (reports deferred) — 2026-08-12

## P3 — Integration & hardening
- [x] ERP integration stubs / staging tables (V17) — 2026-08-13
- [x] Broader integration tests + Testcontainers scaffold — 2026-08-13
- [x] Performance: large BOM roll-up (10k+ nodes, engine) — 2026-08-13
- [x] Full audit coverage review — 2026-08-13 (ERP staging audited + GET /api/v1/audit)
- [x] ERP promote-to-live (V19) — 2026-08-13
- [x] Frontend Audit Log page — 2026-08-13
- [x] Program-scoped BOM promote — 2026-08-13
- [x] ERP Staging management UI — 2026-08-13
- [x] CSV ERP ingest + cancel/reopen lifecycle — 2026-08-13
- [x] Excel .xlsx bulk import (POI) — 2026-08-13
- [x] Machines/labor Excel import parity — 2026-08-13
- [x] Reports & advanced KPIs — 2026-08-13
- [x] Program-scoped reports + CSV export — 2026-08-13
- [x] Flyway V20 reporting/ops indexes — 2026-08-13
- [x] Flyway V21 index hardening — 2026-08-13
- [x] V22 should_cost program_id for program-scoped reports — 2026-08-13

## Done (recent)
- 2026-08-12: P1 complete (Process, Tooling, Should-Cost submit, Approvals entity inbox, Materials versions+bulk)
- 2026-08-12: Workflow StepResponse carries entityType/entityId/workflowInstanceId
- 2026-08-12: GET /api/v1/master/materials/by-code/{code}/versions
- 2026-08-12: Wire CurrencyService into BOM/Should-Cost multi-currency roll-ups (134 tests)
- 2026-08-12: M07 Packaging & Logistics backend (V14, 143 tests)
- 2026-08-12: Target Costing V15 + Dashboard KPIs + Packaging UI (149 tests)
- 2026-08-12: Target Costing frontend (/target-costing) — build green

When completing a task, update PROJECT_STATUS.md + CHANGELOG.md.
- 2026-08-13: RFQ / Supplier frontend (/rfq) — build green
