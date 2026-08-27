# CI/CD Pipeline Integration

**System:** Product Costing + Supplier Intelligence Platform  
**Package baseline:** V278 Universal Costing COMPLETE

## Overview

| Pipeline | File | When |
|----------|------|------|
| GitHub Actions CI | `.github/workflows/ci.yml` | push/PR to main, master, develop, release/* |
| GitHub Release Gate | `.github/workflows/release-gate.yml` | tags `v*` / manual + MySQL Flyway IT |
| GitLab CI | `.gitlab-ci.yml` | MR / branch (mirror of GHA stages) |
| Local / host | `scripts/ci-verify.sh` | Developer or agent host gate |

## Quality gates (must pass)

1. **Backend**
   - JDK 21
   - `mvn -B compile`
   - `mvn -B test-compile`
   - `mvn -B test` (unit suite; Flyway IT skips without DB)
2. **Frontend**
   - Node 22
   - `npm run typecheck`
   - `npm run test` (Vitest)
   - `npm run build`
3. **Optional**
   - Docker image builds (main/master/release)
   - Flyway IT with MySQL service (release-gate)

## Local parity

```bash
./scripts/ci-verify.sh
```

With Flyway IT:

```bash
export TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/product_costing_it?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export TEST_DB_USER=costing_app
export TEST_DB_PASSWORD=changeme
./scripts/ci-verify.sh
```

## Security notes

- Backend remains the authorization boundary (TenantAccessGuard, RBAC, module entitlement).
- CI does not inject production secrets; use GitHub/GitLab protected variables for deploy stages.
- Docker build on CI is **push: false** by default — wire registry credentials in a separate deploy workflow.

## Deploy (out of band)

Production deploy is covered by:

- `PRODUCTION_DEPLOYMENT_RUNBOOK.md`
- `PRODUCTION_DRY_RUN_CHECKLIST.md`
- `PRODUCTION_READINESS_GATE.md`
- `docker-compose.prod.example.yml`

Wire registry push + environment promotion as a separate **CD** workflow after secrets are available.

## Universal Costing coverage

CI runs the full backend unit suite, which includes:

- NormDefinitionServiceTest
- SupplierSobServiceTest
- CostMethodIntegrationFacadeTest
- UniversalCostingServiceTest
- AbcAllocationEngineTest
- roleAccess tests covering `/universal-costing`

No separate job is required unless E2E Playwright is enabled with a live stack.
