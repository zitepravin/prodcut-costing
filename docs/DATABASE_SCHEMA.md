# DATABASE_SCHEMA.md

Managed exclusively by Flyway. Location: `src/main/resources/db/migration/`.

## Applied migrations (V1–V13)

| Version | Focus |
|---------|--------|
| V1 | role, app_user, user_role, access_scope, audit_log (+ grant notes) |
| V2 | Master data: material_master, machine_master, labor_master, overhead_pool, plant, process_master, uom, currency, vehicle_program, ... |
| V3 | bom_node (+ status, make/buy, quantity_per, override, what-if, etc.) |
| V4 | material costing extensions + commodity_index_value + material_cost_calculation; bom_node.material_code |
| V5 | process_routing, process_operation, process_cost_calculation |
| V6 | tooling_record, tooling_cost_calculation, tooling types/status |
| V7 | should_cost_build_up, parametric_cost_benchmark, sga_margin_benchmark |
| V8 | Security / access control refinements |
| V9 | Seed initial admin user + roles |
| V10 | Approval workflow: workflow_definition, step_definition, workflow_instance, approval_step_instance |
| V11 | Should-cost approval status linkage |
| V12 | notification tables |
| V13 | exchange_rate (append-only FX) + currency seed rows (M12 Currency Management) |
| V14 | packaging_logistics_profile + packaging_logistics_cost_calculation (M07) |
| **V15** | **target_cost_record + target_cost_variance (Target Costing)** |

## Conventions

- Primary keys: CHAR(36) UUID strings
- Money / rates / quantities: NUMERIC(18,6) or similar; FX rates NUMERIC(18,8)
- Audit columns on business entities via BaseAuditableEntity (created_by/at, updated_by/at)
- Soft state: status enums + is_active / deactivate patterns; no hard DELETE for master data
- Append-only: audit_log, commodity_index_value, **exchange_rate** (new row per publish)

## Historical note

Intermediate “next free” markers below are historical. Authoritative next free is at end of file.

## V16 — RFQ Management + Supplier Costing (M09/M10)

- `supplier_master` — supplier_code unique, status ACTIVE|INACTIVE|BLOCKED
- `rfq_event` — program_id FK, rfq_number unique, status DRAFT|OPEN|CLOSED|CANCELLED|AWARDED
- `rfq_line` — rfq_id + bom_node_id, line_number, quantity, optional target_unit_price
- `supplier_quote` — unit_price (BigDecimal), status SUBMITTED|UNDER_REVIEW|ACCEPTED|REJECTED|SUPERSEDED|WITHDRAWN
- BUY leaf cost: latest ACCEPTED quote for bom_node_id (BomNodeService)
- SEC-04: `confidentiality_wall.rfq_event_reference` → `rfq_event.rfq_id` (enforced on quote list)


## Important FK / index notes

- bom_node.program_id, parent_node_id, material_code (indexed)
- Material / process / tooling calculations reference bom_node_id
- Workflow instances reference entity type + entity id (polymorphic style)
- exchange_rate.from_currency / to_currency → currency.currency_code; index on (from, to, effective_date)
- packaging_logistics_profile.bom_node_id; packaging_logistics_cost_calculation.bom_node_id + calculated_at
- target_cost_record.bom_node_id / program_id; target_cost_variance.exceeds_threshold

When adding columns, prefer additive ALTER + new tables. Never drop columns that frontend or other modules already depend on without a coordinated migration plan.

## V17 — ERP / PLM staging (stubs)
- `erp_inbound_batch` — source_system, batch_type PART|BOM|MATERIAL_PRICE|MIXED, status RECEIVED→READY/FAILED/PROMOTED
- `erp_part_staging` — part_number, revision, net_weight_kg (CAD/PLM seam), material_code, make_buy_code
- `erp_bom_staging` — parent/child part numbers, quantity_per
- `erp_material_price_staging` — material_code, unit_price, currency_code, effective_from
- No automatic promote to live masters in this version

## V18 — Demo seed data
- Non-schema seed only (users, program, BOM, costing samples, RFQ)

## V19 — ERP promote-to-live
- `erp_inbound_batch.promoted_at`, `promoted_by` (additive)
- Promote job: material prices → MaterialMaster versions; parts acknowledged; **BOM → live BomNode when program_code resolves** (no extra migration)


## V20 — Reporting & operations indexes

Additive indexes only (no column/table changes):

| Index | Table | Purpose |
|-------|-------|---------|
| `idx_target_variance_program` | target_cost_variance | Program-scoped variance KPIs |
| `idx_erp_part_program_code` | erp_part_staging | Promote program resolution |
| `idx_erp_bom_program_code` | erp_bom_staging | Promote program resolution |
| `idx_erp_*_batch_status` | erp_*_staging | Validate by batch + status |
| `idx_labor_status` | labor_master | ACTIVE count for reports |
| `idx_bom_node_program_status` | bom_node | *(added V20, **dropped V21** — redundant)* |
| `idx_audit_action_time` | audit_log | Audit Log filter by action |

*(historical marker — superseded; see end of file)*

## V21 — Index hardening

- **Dropped** `idx_bom_node_program_status` on `bom_node` (redundant with V3 `idx_bom_node_program`; reduces index-merge risk)
- Retained V20 indexes on variance program, ERP program_code / batch+status, labor status, audit action+time

*(historical marker — superseded; see end of file)*

## V22 — should_cost_build_up.program_id

- Additive column `program_id CHAR(36) NULL` on `should_cost_build_up`
- Backfill from `bom_node.program_id`
- Index `idx_should_cost_build_up_program (program_id, calculated_at)`
- Write path: `ShouldCostService` sets `programId` from BOM node on create
- Report aggregates filter `WHERE program_id = ?` (no IN-subquery)

*(historical marker — superseded; see end of file)*

## V64–V70 Vendor evidence depth (2026-08-13)

### V64 `supplier_warranty_claim`
Structured warranty claim history per supplier (optional link to `supplier_warranty_term`).
Statuses: OPEN, ACCEPTED, REJECTED, CLOSED, WITHDRAWN. Evidence lifecycle PENDING→VERIFIED/FAILED/etc.
Indexes: `(supplier_id, claim_date)`, `(warranty_id)`, `(supplier_id, claim_status)`.

### V65 `supplier_guarantee_event`
Guarantee failure/breach/penalty events. Optional `guarantee_id`. `resolved_flag`, penalty amount (BigDecimal).

### V66 `supplier_after_sales_metric`
Period-based after-sales KPIs (response/resolution hours, SLA %, unresolved complaints, support flags).
Separate from warranty tables.

### V67 `vendor_recommendation_explanation.negotiation_opportunities`
VARCHAR(2000) — CALCULATED/MISSING negotiation notes from eligible TCO spread only.

### V68 Questionnaire seed
Additional `vendor_question` rows: VS-Q-AS-*, VS-Q-WC-*, VS-Q-GE-* (idempotent).

### V69 `supplier_performance_feedback`
C3 intake: actual performance feedback. `applied_to_history` requires VERIFIED/WAIVED; never auto-mutates scorecards.

### V70
Index `idx_spf_supplier_applied` on `supplier_performance_feedback (supplier_id, applied_to_history, feedback_date)`.

### V71 `supplier_performance_feedback` history link
- `history_metric_id` VARCHAR(36) NULL
- `history_metric_kind` VARCHAR(20) NULL — QUALITY | DELIVERY when a metric row was created
- Index `idx_spf_history_metric (history_metric_kind, history_metric_id)`

## V72–V74 indexes
- V72 `idx_sge_unresolved` on supplier_guarantee_event (resolved_flag, supplier_id, event_date)
- V73 `idx_sr_supplier_status_severity` on supplier_risk (supplier_id, status, severity)
- V74 `idx_spf_evidence_kpi` on supplier_performance_feedback (evidence_status, feedback_date)

## V75
- Index `idx_swc_evidence_status` on supplier_warranty_claim (supplier_id, evidence_status, claim_date)

## V76 vendor_comparison indexes
- `idx_vc_program_created` (program_id, created_at DESC)
- `idx_vc_created` (created_at DESC)

## V77
- Index `idx_sc_status_valid_to` on supplier_capability (status, valid_to)

## V78
- Index `idx_sas_evidence_status` on supplier_after_sales_metric (supplier_id, evidence_status, period_end)

## V79
- Index `idx_sqm_evidence_status` on supplier_quality_metric (supplier_id, evidence_status, period_end)

## V80
- Index `idx_sdm_evidence_status` on supplier_delivery_metric (supplier_id, evidence_status, period_end)

## V81
- Index `idx_sge_evidence_status` on supplier_guarantee_event (supplier_id, evidence_status, event_date)

## V82
- Index `idx_swc_status_evidence` on supplier_warranty_claim (supplier_id, claim_status, evidence_status, claim_date)

## V83–V86
- V83: idx_swc_updated_at on supplier_warranty_claim
- V84: idx_sr_status_identified on supplier_risk
- V85: idx_spf_evidence_date on supplier_performance_feedback
- V86: idx_sqc_lifecycle on supplier_qualification_current

## V87
- Index `idx_audit_entity_action_time` on audit_log (entity_type, action, timestamp)
- audit_log.action values include TRANSITION, VERIFY (in addition to CREATE/UPDATE/DELETE)

## V88
- Index `idx_sge_resolved_evidence` on supplier_guarantee_event (supplier_id, resolved_flag, evidence_status, event_date)

## V89
- Index `idx_swc_claim_status_only` on supplier_warranty_claim (claim_status)
- Index `idx_spf_evidence_only` on supplier_performance_feedback (evidence_status)

## Authoritative migration state (2026-08-14)

- Highest applied migration in V91 packages: **V89**
- **Next free migration: V91**
- Create V90 only when an approved schema change is required.
- Conventions: CHAR(36) UUID PKs/FKs (utf8mb4_unicode_ci), NUMERIC for money, additive migrations, idempotent index creation where practical.

## V90 — ID column CHAR(36) alignment (2026-08-14)

Normalize UUID ID columns from `VARCHAR(36)` to `CHAR(36)` for Hibernate `ddl-auto=validate` and FK consistency with `supplier_master.supplier_id` (CHAR).

**Tables modified (PK / supplier_id / key FKs):**
- `cost_version_record` (version_id, entity_id)
- `supplier_capability`, `supplier_eligibility_result`
- `supplier_quality_metric`, `supplier_delivery_metric`
- `supplier_warranty_term`, `supplier_guarantee_term`
- `supplier_warranty_claim`, `supplier_guarantee_event`
- `supplier_after_sales_metric`, `supplier_performance_feedback` (incl. history_metric_id)

`created_by` / `updated_by` left as VARCHAR(36) (no FK).  
`FOREIGN_KEY_CHECKS` toggled during MODIFY (MySQL 8).

**Next free migration: V91**

### V97 `purchase_request` / `purchase_request_line` (P3-01)
Purchase intent after human Make-vs-Buy APPROVE_BUY/MIXED (or manual override reason).
- Status: DRAFT → SUBMITTED → APPROVED/REJECTED → RELEASED
- `data_status` / `tco_status`: AVAILABLE | NOT_AVAILABLE (never invent FX/price)
- Module entitlement: `PURCHASE` (seeded V32)
- Export package is read-only; does not post to SAP MM
