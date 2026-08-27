# V292 Dashboard — Role Demo User Matrix

| Field | Value |
|-------|-------|
| **Date** | 2026-08-24 |
| **Seed** | `V166__dashboard_role_demo_users.sql` |
| **Password (all demo accounts)** | `ChangeMe!Now1` (existing V9/V18 BCrypt; development placeholder only) |
| **Tenant** | `DEFAULT` via `customer_user_membership` |
| **Live login** | **NOT EXECUTED** in this environment (no running app/MySQL here) |

Do not treat seed completeness as live Dashboard PASS.

## Role coverage

| Role | Username | Tenant | Dashboard persona | Seed | Live login |
|------|----------|--------|-------------------|------|------------|
| SYSTEM_ADMINISTRATOR | `demo.systemadmin` | DEFAULT | SYSTEM_ADMINISTRATION | YES | NOT EXECUTED |
| MASTER_DATA_ADMIN | `demo.master` | DEFAULT | MASTER_DATA | YES (reused V18) | NOT EXECUTED |
| COSTING_ENGINEER | `demo.engineer` | DEFAULT | COST_ENGINEERING | YES (reused V18) | NOT EXECUTED |
| COSTING_MANAGER | `demo.manager` | DEFAULT | COST_MANAGEMENT | YES (reused V18) | NOT EXECUTED |
| FINANCE_CONTROLLER | `demo.finance` | DEFAULT | FINANCE | YES | NOT EXECUTED |
| PURCHASING_BUYER | `demo.buyer` | DEFAULT | PURCHASING_OPERATIONS | YES (reused V18) | NOT EXECUTED |
| SOURCING_LEAD | `demo.sourcing` | DEFAULT | SOURCING_MANAGEMENT | YES | NOT EXECUTED |
| PROGRAM_COST_MANAGER | `demo.programcost` | DEFAULT | PROGRAM_COSTING | YES | NOT EXECUTED |
| PROCESS_ENGINEER | `demo.process` | DEFAULT | PROCESS_ENGINEERING | YES | NOT EXECUTED |
| TOOLING_ENGINEER | `demo.tooling` | DEFAULT | TOOLING | YES | NOT EXECUTED |
| COMMODITY_MATERIAL_BUYER | `demo.materialbuyer` | DEFAULT | MATERIAL_PROCUREMENT | YES | NOT EXECUTED |
| INDUSTRIAL_ENGINEER | `demo.industrial` | DEFAULT | INDUSTRIAL_ENGINEERING | YES | NOT EXECUTED |
| PRODUCT_COST_ENGINEER | `demo.productcost` | DEFAULT | PRODUCT_COSTING | YES | NOT EXECUTED |
| PURCHASING_MANAGER | `demo.purchasemanager` | DEFAULT | PURCHASING_MANAGEMENT | YES | NOT EXECUTED |
| PLATFORM_OWNER | `demo.platformowner` | DEFAULT | PLATFORM_ADMINISTRATION | YES | NOT EXECUTED |
| CUSTOMER_ADMIN | `demo.customeradmin` | DEFAULT | CUSTOMER_ADMINISTRATION | YES | NOT EXECUTED |

Also present (not a Dashboard persona user): `admin` (V9) = SYSTEM_ADMINISTRATOR bootstrap.

**Not created:** `APPROVAL_OVERRIDE` (permission, not a Dashboard persona).

## Existing users reused

| Username | Role | Source |
|----------|------|--------|
| `demo.engineer` | COSTING_ENGINEER | V18 |
| `demo.manager` | COSTING_MANAGER | V18 |
| `demo.buyer` | PURCHASING_BUYER | V18 |
| `demo.master` | MASTER_DATA_ADMIN | V18 |

Naming follows V18 (`demo.engineer`, not `demo.costengineer`).

## New users created (V166)

`demo.systemadmin`, `demo.finance`, `demo.sourcing`, `demo.programcost`, `demo.process`, `demo.tooling`, `demo.materialbuyer`, `demo.industrial`, `demo.productcost`, `demo.purchasemanager`, `demo.platformowner`, `demo.customeradmin`

Each has **exactly one** `user_role` row. No extra PLATFORM_OWNER / SYSTEM_ADMINISTRATOR grants.

## Tenant assignments

All 16 Dashboard demo users are ACTIVE members of customer `DEFAULT` (same as V33).  
`CustomerContext` requires membership; this does not disable tenant isolation.

DEFAULT module entitlements (already seeded, not widened here):

- PRODUCT_COSTING (V32)
- VENDOR_SELECTION (V32)
- PURCHASE, ORDER_TRACKING (V122)

## Permissions

Role → permission comes from existing `PermissionMatrixService`. All 16 roles include `DASHBOARD_VIEW`. No extra grants were added to make widgets appear.

## Demo data (existing V18+)

Program `EV-CX-2027`, BOM MAKE/BUY, should-cost, materials, RFQ, suppliers ACME/NIPPON, plants DET-01/PUN-01. No invented KPI numbers.

## Files changed

| File | Change |
|------|--------|
| `backend/src/main/resources/db/migration/V166__dashboard_role_demo_users.sql` | **New** seed |
| `docs/DEMO_WALKTHROUGH.md` | Login table expanded |
| `docs/v292-merge/ROLE_DEMO_USER_MATRIX.md` | This matrix |

## How to apply on the live DB

If Flyway already ran through V165:

```bat
cd backend
mvn flyway:migrate
```

or restart `mvn spring-boot:run` so Flyway applies V166.

Then login each user with password `ChangeMe!Now1` and tenant **DEFAULT**.

## Live Dashboard checklist (for the operator)

For each row above: login → confirm role → DEFAULT tenant → open Dashboard → persona/widgets/quick actions → logout.

This environment did **not** run those logins.
