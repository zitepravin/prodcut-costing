---
name: universal-costing
description: Industry-neutral universal costing for V278/V279. Use when implementing or reviewing cost objects, cost methods, cost drivers, ABC allocation, industry profiles, job/process/project/service costing, or when avoiding automotive-only assumptions. Triggers include universal costing, industry-neutral costing, cost object, cost driver, ABC, job order costing, process costing, WBS, recipe costing, lifecycle cost.
metadata:
  version: "V278"
  framework: industry-neutral-universal-costing
---

# Universal Costing Skill (V278)

Apply this skill for any costing work on PRODUCT-COSTING-SOFTWARE-MASTER-V278 and later.

## Core rule

Industry = **configuration profile**, never a separate codebase branch.

Do **not** write `if (automotive)`, `if (pharma)`, or industry-specific engine forks.
Use Cost Object + Cost Method + Driver + Component + Allocation + Version/Freeze.

## Architecture

```text
UNIVERSAL COSTING CORE
  → COST METHODS
  → COST COMPONENTS
  → COST DRIVERS / POOLS
  → ALLOCATION (including ABC)
  → CALCULATION PIPELINE + SNAPSHOT
  → VERSION / APPROVAL / FREEZE
  → INDUSTRY PROFILE (template only)
  → CUSTOMER CONFIGURATION (entitlement + RBAC)
```

Package (implemented): `com.automotive.costing.universal.*`
APIs: `/api/v1/universal-costing/**`
Migration: `V151__universal_costing_engine.sql`

## Cost methods (feature-gated)

STANDARD, ACTUAL, ESTIMATED, SHOULD, TARGET, JOB, PROCESS, ABC,
AVERAGE, QUOTATION, PROJECT, LIFECYCLE, HYBRID

Feature codes: `COST_METHOD_<NAME>`, plus
`COST_DRIVER_CONFIGURATION`, `COST_POOL_CONFIGURATION`,
`COST_ALLOCATION_CONFIGURATION`, `INDUSTRY_PROFILE_CONFIGURATION`

## Cost objects (not only product)

PRODUCT, MATERIAL, SERVICE, JOB, WORK_ORDER, PROJECT, WBS, BATCH,
PROCESS, CONTRACT, CUSTOMER_ORDER, ASSET, MAINTENANCE_JOB,
COST_CENTER, DEPARTMENT, ACTIVITY

## Calculation pipeline

1. Resolve scope (tenant, object, method)
2. Assert feature entitlement + CONFIG_* RBAC where applicable
3. Resolve effective configuration / rates / structure
4. Calculate direct components (`CostComponentResult`)
5. Allocate indirect (ABC when method is ABC/HYBRID)
6. Apply scrap/yield if configured
7. FX with explicit `asOfDate` when required
8. Total + unit cost
9. Persist snapshot (DRAFT → APPROVED/ACTIVE → LOCKED)

ABC formula (deterministic, BigDecimal HALF_UP):

```text
Driver Rate = Cost Pool ÷ Total Driver Quantity
Allocated   = Driver Rate × Object Driver Usage
```

Reject zero/negative driver quantity or negative usage.

## Reuse existing V278 engines

| Need | Reuse |
|------|--------|
| Should cost | `ShouldCostService` / engines |
| Standard | `StandardCostService` |
| Target | `targetcosting` |
| Process components | `ProcessCostEngine` |
| Actual | `ActualCostService` (tenant-scoped) |
| FX | `CostCurrencyConverter` with asOfDate |
| Config editability | V150 `configuration_definition.editable` |
| Security | Module → Feature → CONFIG_* → TenantAccessGuard |

Enhance these; do not duplicate parallel engines for the same math.

## Industry profiles

Profiles in `industry_costing_profile` are **templates only** (methods, components, drivers defaults).
Examples: DISCRETE_MANUFACTURING, PROCESS_MANUFACTURING, ELECTRONICS,
PROFESSIONAL_SERVICES, CONSTRUCTION, LOGISTICS, PHARMA, MAINTENANCE_REPAIR.

## Security non-negotiables

- Platform entitlement = WHAT capabilities exist
- Customer Admin role/permission = WHO may use them
- Customer cannot change `editable=false` definitions
- Approved/ACTIVE calculation values must not mutate on later config change
- Never claim production-ready without dual-tenant HTTP, load, and DR evidence

## What not to do

- Do not hard-code automotive BOM as the only structure
- Do not invent actual costs from should/standard
- Do not auto-approve costing
- Do not weaken TenantAccessGuard or feature gates
- Do not redesign completed FX/should-cost pipelines

## References

- Project report: `V278_Industry_Neutral_Universal_Costing_Framework_Report.docx`
- Detailed dimension catalog: `references/framework-dimensions.md`
