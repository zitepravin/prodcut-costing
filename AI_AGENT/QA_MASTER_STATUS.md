# QA_MASTER_STATUS.md

**Session:** 2026-08-14 Docker-free full-scale testing  
**Code base:** MASTER V120 + V121 aspect fix  
**Environment:** MariaDB 10.11 host (no Docker), Temurin JDK 21, Maven 3.9.9, Node 24

| Batch | Scope | Result | Count | Notes |
|-------|--------|--------|-------|-------|
| B1 | Backend unit (exclude integration.*) | **PASS** | 432 / 0 fail | Surefire BUILD SUCCESS |
| B2 | Flyway V1→V90 on host MariaDB | **PASS** | V90 success | Via Spring Boot startup |
| B3 | Spring Boot start (local profile) | **PASS** | — | Port 8080 |
| B4 | Auth login API | **PASS** | HTTP 200 | admin / ChangeMe!Now1 |
| B5 | CAM platform customers/modules | **PASS** | HTTP 200 | |
| B6 | Module ACCESS-07 (no X-Customer-Id) | **PASS** | HTTP 403 TENANT_ACCESS_DENIED | |
| B7 | Module with header (VS 360, materials) | **PASS** after fix | HTTP 200 | Required aspect JoinPoint fix |
| B8 | Module ACCESS-03 (VS disabled) | **PASS** after fix | HTTP 403 MODULE_NOT_ENABLED | |
| B9 | RequiresModuleAspectTest | **PASS** | 3 / 0 fail | |
| B10 | Frontend Vitest | **PASS** | 17 / 0 fail | |
| B11 | Frontend tsc | **PASS** | clean | |
| B12 | Playwright smoke + VS suites | **PASS** | 12 / 0 fail | |
| B13 | Playwright module-entitlement | **PASS** after path fix | 2 / 0 fail | |
| B14 | FlywayMigrationIntegrationTest | **BLOCKED** | 1 skipped | DOCKER REQUIRED |
| B15 | Full Playwright after final code | NOT RUN | — | Env reset mid-session; B12–B13 already green |

## Defects found and fixed this session

1. **P0** Class-level `@RequiresModule` AOP binding → HTTP 500 when `X-Customer-Id` present  
   - Root cause: annotation parameter not resolved for `@within`  
   - Fix: JoinPoint-based resolve from method/class/superclass  
   - Verified: ACCESS-07, ACCESS-03, 200 when enabled  

2. **P2** `module-entitlement.spec.ts` used non-existent `/vendor-selection/suppliers` list path  
   - Fix: use seed supplier 360 path  

## Next QA
- Re-run full unit suite + Playwright after V121 package on stable host
- Docker IT when Docker available
