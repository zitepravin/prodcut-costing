# SESSION V192 — Backend testCompile fix (ShouldCostServiceTest)

**Date:** 2026-08-15  
**Type:** Defect fix (customer docker compose build)

## Customer failure (V191)
Main sources compiled. Docker still failed on **testCompile**:

```
ShouldCostServiceTest.java: constructor ShouldCostService cannot be applied
required: ... ShouldCostEmissionsService ...
found:    ... (missing ShouldCostEmissionsService)
```

Note: `-DskipTests` skips **running** tests, not **compiling** them.

## Fix
`ShouldCostServiceTest.java`:
- `@Mock ShouldCostEmissionsService shouldCostEmissionsService`
- Pass into `new ShouldCostService(..., buildUpRepository, shouldCostEmissionsService, auditService, ...)`

## Verification
`mvn -DskipTests package` → **BUILD SUCCESS** (includes testCompile)

## Carry-forward from V191
- ResourceNotFoundException 2-arg constructor
- MakeVsBuyComparisonEngine.UNKNOWN
- Frontend V190 type fixes
