# Table Tenant Classification (MASTER-V254 → V255 workstream)

**Purpose:** Decide which tables are Tenant-Owned, Shared Platform, or Hybrid before applying `customer_id`.

**Rule:** Tenant-Owned transactional data must never be readable or writable across customers.  
Authorization must use **server-side** `CustomerContext` (from membership), never a client-only `customer_id` claim.

---

## SHARED PLATFORM (no customer_id)

| Table | Reason |
|-------|--------|
| platform_customer | Tenant registry itself |
| platform_module | Module catalog |
| role | System RBAC roles |
| user_role | Global user↔role (membership is separate) |
| app_user | Users may belong to multiple customers via membership |
| billing_plan / billing_plan_module | Commercial catalog |
| notification_template (if global) | Optional shared templates |
| uom_master | Global UOM reference (product decision: keep shared) |
| currency (reference) | ISO currency list if present as ref data |
| domain_outbox | Already has nullable customer_id for routing |

---

## TENANT-OWNED (must have customer_id) — Wave 1 (this migration)

Highest business risk / visible customer data:

| Table | Notes |
|-------|--------|
| vehicle_program | Root of costing hierarchy |
| bom_node | BOM tree; scoped via program + direct customer_id |
| plant_master | Plants are customer-specific in SaaS |
| material_master | Tenant material catalog |
| machine_master | Tenant machines |
| labor_master | Tenant labor rates |
| process_master / process_operation / process_routing | Tenant process definitions |
| should_cost_build_up / should_cost_quote_component | Costing results |
| supplier_master | Tenant supplier directory |
| rfq_event / rfq_line | RFQ |
| supplier_quote | Quotes |
| purchase_request | Already has nullable customer_id → harden |
| purchase_order | Already has NOT NULL customer_id |
| vendor_comparison / vendor_recommendation* | Vendor selection outputs |
| costing_run / costing_run_item | Runs |
| make_vs_buy_simulation | Simulations |
| stored_file | Already NOT NULL customer_id (V130) |
| tenant_costing_defaults | Already keyed by customer |
| customer_* platform tables | Already tenant-scoped |

---

## TENANT-OWNED — Wave 2 (follow-on migrations)

| Area | Tables (examples) |
|------|-------------------|
| Vendor quality/delivery/TCO | supplier_quality_metric, supplier_delivery_metric, supplier_tco_calculation, supplier_risk, supplier_warranty_*, supplier_capability, supplier_qualification* |
| Target / variance / standard | target_cost_*, standard_cost, actual_cost_record |
| Packaging / tooling / process cost calcs | packaging_*, tooling_*, process_cost_calculation, material_cost_calculation |
| Workflow instances | approval_workflow_instance (add customer_id for isolation) |
| Notifications | app_notification, notification_delivery (when not already scoped) |
| ERP staging | erp_* staging batches |
| Benchmarks | parametric_cost_benchmark, sga_margin_benchmark (if tenant-specific) |
| Emissions | emission_factor (if tenant overrides), should_cost_emissions_snapshot |
| Scorecards / questionnaires | vendor_scorecard_*, vendor_question*, questionnaire_* |

---

## HYBRID

| Table | Policy |
|-------|--------|
| exchange_rate | Shared market rates OR tenant overrides — start shared; tenant override table later if needed |
| emission_factor | Platform defaults + optional tenant overrides (wave 2) |
| workflow_definition | May start shared templates; tenant copies later |

---

## Uniqueness rules after tenant column

Global unique keys that become **per-tenant**:

- `vehicle_program.program_code` → `(customer_id, program_code)`
- `supplier_master.supplier_code` → `(customer_id, supplier_code)`
- Similar for plant codes, material codes where currently global UNIQUE

---

## Enforcement layers (all required)

1. **Schema:** `customer_id` column + composite indexes  
2. **Write path:** set `customer_id` from `CustomerContext` only  
3. **Read path:** every query filtered by current customer (repository method or Hibernate filter)  
4. **Tests:** Customer A token + Customer B id → 403 / empty / not found — never 200 with B’s data  

---

## Backfill strategy

Existing single-tenant deployments:

```sql
UPDATE <table>
SET customer_id = (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1)
WHERE customer_id IS NULL;
```

Then tighten to `NOT NULL` where no legitimate null remains.


## Status updates

- **V135 (MASTER-V265):** `customer_id` enforced on `material_master`, `machine_master`, `labor_master` with service/repository tenant filters. Classification decision (tenant-owned) confirmed and implemented.


## Questionnaire catalog decision (MASTER-V268)

| Artifact | Ownership | Rationale |
|----------|-----------|-----------|
| `vendor_question` (question bank) | **Tenant-owned** (classification wave 2) | Scoring rules and mandatory gates differ by OEM customer; shared global questions risk leaking competitive criteria |
| `vendor_questionnaire_response` | Tenant-owned | DONE V137 |
| `vendor_questionnaire_evaluation` | Tenant-owned | DONE V137 |

**Implementation note:** Question list APIs should filter by `customer_id` in a follow-on pass (V139). Until then, responses/evaluations remain isolated; question *text* may still be globally listed — treat as residual risk for multi-tenant go-live.


- **V139 (MASTER-V269):** `vendor_question.customer_id` + unique `(customer_id, question_code, version_no)`; listActiveQuestions tenant-filtered.
