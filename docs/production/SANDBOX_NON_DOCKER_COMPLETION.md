# Sandbox non-Docker completion (MASTER-V275)

## Completed without Docker

| Item | Status |
|------|--------|
| Table classification (153) | DONE |
| Core domain customer_id waves V130–V143 | DONE |
| TenantAccessGuard + CustomerContext | DONE |
| ProductionHardeningValidator | DONE |
| Redis rate limit code path + prod force | DONE |
| IdempotencyKeyFilter | DONE |
| Max page size 100 | DONE |
| Questionnaire / evidence / ERP promote tenant fixes | DONE |
| Catalog seed on customer create | DONE |
| Cross-tenant pure unit tests | ADDED |
| ProductionHardeningValidator unit tests | ADDED |
| docker-compose.prod.example.yml | DONE |
| EXTERNAL_DOCKER_TEST_INSTRUCTIONS | DONE |

## Explicitly pending (requires external host)

| Item | Why |
|------|-----|
| docker compose up | No Docker in sandbox |
| Maven test run | No mvn in sandbox |
| k6 load | Needs running stack |
| Playwright E2E | Needs running stack |
| Live cross-tenant HTTP | Needs running stack |
| DR restore timed | Needs MySQL ops |
| Multi-instance Redis proof | Needs multi-node |

## Status

**SANDBOX SOURCE WORK COMPLETE**  
**READY FOR EXTERNAL DOCKER TESTING**  
**NOT PRODUCTION READY**
