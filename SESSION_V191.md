# SESSION V191 — Backend Docker compile fix (ResourceNotFoundException + UNKNOWN)

**Date:** 2026-08-15  
**Type:** Defect fix (customer docker compose build backend failed)

## Customer failure (V190)
`docker compose build` → backend `mvn package` compilation errors:

1. `ResourceNotFoundException` — many Phase-3 services call `new ResourceNotFoundException(entityType, id)` but class only had 1-arg constructor.
2. `MakeVsBuyComparisonEngine.UNKNOWN` — used by `MakeVsBuyService` but constant missing.

## Fixes
| File | Change |
|------|--------|
| `common/exception/ResourceNotFoundException.java` | Added 2-arg constructor `(String entityType, Object id)`; `of()` delegates to it |
| `makevsbuy/engine/MakeVsBuyComparisonEngine.java` | Added `public static final String UNKNOWN = "UNKNOWN"` |

## Verification
- Host: `mvn -DskipTests compile` → **BUILD SUCCESS** (555 sources, JDK 21)
- No business-rule changes; eligibility/missing-data behavior unchanged

## Not changed
- Frontend (V190 FE fixes remain)
- FX / AI approve / SAP MM / RELEASE on NOT_AVAILABLE policies
