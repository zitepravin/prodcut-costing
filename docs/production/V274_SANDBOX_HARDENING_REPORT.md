# V274 Sandbox Hardening Report (no Docker execution)

**Package:** PRODUCT-COSTING-SOFTWARE-MASTER-V274  
**Rule:** Docker was not available and was not run.

## Final status

### READY FOR EXTERNAL DOCKER TESTING

Sandbox source hardening is complete enough to move the ZIP to a Docker-capable environment.  
**Not** production-ready. **Not** load-tested.

---

### A. Files changed (high level)

- QuestionnairePackService — tenant mandatory questions
- EvidenceExpiryService — scoped policy status query
- MaxPageSizeWebConfig — page size ≤ 100
- IdempotencyKeyFilter + SecurityConfig wiring
- docker-compose.yml marked LOCAL ONLY
- docker-compose.prod.example.yml (secrets, Redis, no public MySQL)
- TenantAccessGuardCrossTenantTest
- EXTERNAL_DOCKER_TEST_INSTRUCTIONS.md

### B. Database changes

- None new in V274 (V143 already in V273 for cost/RFQ children)
- Classification remains TABLE_TENANT_CLASSIFICATION_V272.md

### C. Tenant isolation

- Pack mandatory-gate check uses tenant question bank
- Evidence expiry no longer full table scan for policies
- Cross-tenant unit test (no DB)

### D. Security

- Idempotency-Key filter on purchase/approval/RFQ/field-sync paths
- Prod fail-fast validator retained from V273

### E. Scalability

- Max page size 100
- Idempotency reduces duplicate writes under retry

### F. Tests actually executed

| Test | Result |
|------|--------|
| Docker compose | **NOT RUN** (unavailable) |
| Maven unit suite | **NOT RUN** (mvn not installed in sandbox) |
| TenantAccessGuardCrossTenantTest | **ADDED** (executable when JUnit available) |
| Static code review of findAll/findById residuals | **DONE** |

### G. Tests NOT executable (Docker/tooling)

- docker compose up
- Playwright against containers
- k6 against containers
- Multi-instance Redis rate limit
- Live cross-tenant HTTP matrix
- DR restore
- Full Maven/Spring context tests (no mvn)

### H. Remaining issues

| Priority | Item |
|----------|------|
| P0 | Live cross-tenant HTTP evidence |
| P0 | Load test evidence |
| P0 | Multi-instance Redis proof |
| P1 | EvidenceExpiry warranty/guarantee still may scan broadly |
| P1 | Billing findAll (platform admin — acceptable with authz) |
| P1 | ErpPromote customer stamp verification under integration test |
| P2 | Field offline sync (not claimed) |

### I. Docker instructions

See `docs/production/EXTERNAL_DOCKER_TEST_INSTRUCTIONS.md`

### J. Status line

**READY FOR EXTERNAL DOCKER TESTING**
