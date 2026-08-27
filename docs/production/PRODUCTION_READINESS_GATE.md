# Production Readiness Gate Checklist

**Target capacity:** ~1,000 customers · 2,000–5,000 concurrent users  
**Package baseline:** MASTER-V270  

Status legend: **DONE** · **PARTIAL** · **OPEN** · **BLOCKED**

---

## 1. Multi-tenancy & security

| Criterion | Status | Notes |
|-----------|--------|-------|
| Access-layer tenant checks (X-Customer-Id + module entitlements) | DONE | CentralAccess / CrossTenantApiTest |
| Data-plane `customer_id` wave-1 (program, BOM, supplier, RFQ, should-cost, PR/PO) | DONE | V131 |
| Data-plane wave-2 (metrics, TCO, risk, quote, workflow, notifications) | DONE | V132 |
| Scorecard / comparison `customer_id` | DONE | V133 |
| Service enforcement (VehicleProgram, BomNode, Supplier, RFQ, PR, PO, VS core) | DONE | TenantAccessGuard pattern |
| Warranty/claim/guarantee/event/policy evidence paths | DONE | V134 + tenant repos |
| Masters material/machine/labor/process/plant/tooling | DONE | V135–V140 |
| Questionnaire bank + responses | DONE | V137/V139 |
| Email multi-instance claim | DONE | FOR UPDATE SKIP LOCKED |
| Login + API rate limit Redis-capable | DONE | RATE_LIMIT_STORE=redis |
| No `findAll()` / bare `findById` on tenant tables in hardened services | PARTIAL | Ongoing — remaining edge lists |
| Cross-tenant isolation integration tests (two customers, data plane) | OPEN | Needs Testcontainers dual-tenant suite |
| Login rate limit multi-instance safe | PARTIAL | API rate limit Redis-capable; login still in-memory |
| API rate limit multi-instance safe | DONE* | `RATE_LIMIT_STORE=redis` |
| Production JWT + CORS explicit | DONE | Production profile requirements |

\*Must deploy with Redis for multi-instance.

---

## 2. Scalability & reliability

| Criterion | Status | Notes |
|-----------|--------|-------|
| Stateless JWT backends (horizontal scale) | DONE | |
| Hikari pool sizing via env | PARTIAL | Documented formula; verify under multi-instance |
| Transactional outbox table + enqueue | DONE | V130 |
| Outbox worker SKIP LOCKED + retry/DEAD | DONE | |
| Outbox external publisher | PARTIAL | logging / spring-event; broker TBD |
| Object storage (not MySQL LOBs) | DONE | local + S3 providers |
| Shared object storage across replicas | PARTIAL | Requires `STORAGE_PROVIDER=s3` |
| Email queue multi-instance claim | OPEN | DB-polled; needs same SKIP LOCKED pattern as outbox |
| Noisy-neighbor limits (report/import/AI) | PARTIAL | Rate buckets; queue depth limits TBD |

---

## 3. Observability & ops

| Criterion | Status | Notes |
|-----------|--------|-------|
| Correlation IDs | DONE | |
| Actuator + Prometheus metrics | PARTIAL | Protect exposure in prod |
| Outbox processed metrics | DONE | `pcs.outbox.processed` |
| Structured logging | PARTIAL | Correlation in MDC |
| Automated backups | OPEN | Ops-owned |
| Tested restore (RPO≈15m RTO≈60m) | OPEN | Runbook exists; drill not evidenced |
| Load test evidence at target concurrency | OPEN | k6 script present; not executed here |

---

## 4. Acceptance criteria (original hardening brief)

| # | Criterion | Status |
|---|-----------|--------|
| 1 | 1,000 customer tenant isolation | PARTIAL — schema + services; full proof OPEN |
| 2 | Horizontal backend scaling | PARTIAL — architecture ready; load proof OPEN |
| 3 | No single backend SPOF | PARTIAL — needs LB + multi-instance + Redis + S3 |
| 4 | Database connection protection | PARTIAL — Hikari env; pool math OPEN |
| 5 | Tenant-aware rate limiting | DONE with Redis |
| 6 | Async long-running workloads | PARTIAL — outbox; more queues TBD |
| 7 | Reliable email queue | OPEN |
| 8 | Transactional outbox where required | DONE for PO approve/issue |
| 9 | Object storage for large files | DONE |
| 10 | Idempotent critical APIs | OPEN — not systematic |
| 11 | Field sync strategy | OPEN |
| 12 | Production monitoring | PARTIAL |
| 13 | Structured logging + correlation | PARTIAL / DONE |
| 14 | Automated backups | OPEN |
| 15 | Tested restoration | OPEN |
| 16 | Security testing | PARTIAL — unit isolation tests |
| 17 | Load / performance testing | OPEN |
| 18 | Failure/recovery testing | OPEN |
| 19 | No known critical cross-tenant issue | PARTIAL — residual unscoped paths possible |

---

## 5. Go / No-Go

**Do NOT declare production-ready** until:

1. Dual-tenant data-plane isolation tests pass  
2. Load test at ≥2k concurrent with p95 targets measured  
3. DR restore drill completed and timed ≤ RTO  
4. Redis rate limit + S3 (or equivalent) in the target environment  
5. Remaining `findById` scans on tenant tables closed or risk-accepted  

**Architecture direction remains:** modular monolith + horizontal scale; no microservices without measured need.


## Evidence capture (ops)

- Load: `docs/production/LOAD_TEST_EVIDENCE_TEMPLATE.md`
- DR: `docs/production/DR_DRILL_EVIDENCE_TEMPLATE.md`
