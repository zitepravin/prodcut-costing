# FEATURE P3-07 — Customer Configuration Center (PROPOSED REQUIREMENT)

**Document type:** PROPOSED REQUIREMENT  
**Gap ID:** P3-07  
**Date:** 2026-08-15  
**Status:** **DESIGN APPROVED** — §11 locked; **P3-07a IMPLEMENTED** in MASTER-V184; 7b–7e not started  
**Authorization:** `AUTHORIZE P3-07 DESIGN ONLY`  
**Baseline package:** MASTER-V182 (Flyway V100)

---

## 1. Business purpose

Customers (tenants) need to **configure the platform to their process** without code changes:

| Config area | Business need |
|-------------|----------------|
| **Module packs** | Enable only the modules they bought / need |
| **Versioned scorecard weights** | Commodity / plant-specific VS category weights (must total 100%) |
| **Questionnaire packs** | Active question sets by commodity / region / purpose |
| **Workflow routing** | Who approves what by value / entity type |
| **Costing defaults** | Reporting currency, default overhead/scrap defaults (where allowed) |

This answers:

> Can each customer shape scoring, questionnaires, approvals, and defaults to match their governance — without weakening mandatory human control or data integrity?

---

## 2. Explicit non-goals (frozen)

| Non-goal | Reason |
|----------|--------|
| Customer disables human approval of suppliers / PR / MvB | Master Spec §26–§31 |
| Customer enables AI auto-approve / auto-award | Policy |
| Customer invents FX or forces missing cost to zero | Integrity |
| Customer turns on live SAP MM posting from app | Boundary (CPI only) |
| Free-form script injection / arbitrary formula engine (v1) | Security & audit risk |
| Cross-tenant config copy without explicit export/import job | Tenant isolation |
| Changing historical evaluation scores by editing active weights | Versioning — historical snapshots stay linked to weight **version** used at evaluation time |

**AI role:** help explain config impact only. **Never** publish config without human admin action.

---

## 3. Actors & security

| Role | Capability |
|------|------------|
| **SYSTEM_ADMINISTRATOR / Platform Admin** | Module registry; support; break-glass audit |
| **Customer Admin** (tenant) | Entitlements within contract; scorecard templates; questionnaire packs; workflow defs; costing defaults |
| **Cost / VS analyst** | **Read** effective config; cannot publish |
| **Helping AI** | Read-only explanation of effective config |

All APIs: tenant isolation. Config publish requires authenticated human admin.

**Proposed module for UI:** existing platform admin **or** new `CONFIGURATION` entitlement — open decision §11.

---

## 4. Configuration domains

### 4.1 Module packs

**Foundation:** Central Access entitlements (`PRODUCT_COSTING`, `VENDOR_SELECTION`, `PURCHASE`, …).

| Concept | Description |
|---------|-------------|
| **Pack** | Named bundle of module codes (e.g. `COSTING_ONLY`, `COSTING_PLUS_VS`, `FULL_SOURCING`) |
| **Contracted set** | Max modules a tenant may enable (billing) |
| **Effective entitlement** | contracted ∩ customer-enabled |

**Rules:** cannot enable outside contract; disable does not delete history; PURCHASE off hides nav + API (existing F).

### 4.2 Versioned scorecard weights

**Foundation:** `ScorecardScoringEngine` — weights supplied by caller; must total **100%**.

| Entity | Fields (conceptual) |
|--------|---------------------|
| `scorecard_weight_set` | id, tenant_id, code, name, status (`DRAFT`/`PUBLISHED`/`RETIRED`), version_no, effective_from/to, commodity_family nullable, plant_code nullable |
| `scorecard_weight_line` | set_id, category_code, category_name, weight_percent |

**Rules:**
- Publish only if weights sum to 100%.
- Evaluations **bind** to weight_set version (snapshot).
- No edit of PUBLISHED — new version only.
- System default set optional (open decision).

### 4.3 Questionnaire packs

**Foundation:** `vendor_question` versioned bank; responses pin question_version.

| Entity | Description |
|--------|-------------|
| `questionnaire_pack` | id, tenant_id, code, name, purpose, status, version |
| `questionnaire_pack_item` | pack_id, question ref, sort_order |

**Rules:** publish freezes items; evaluation references pack version; mandatory gates remain enforced.

### 4.4 Workflow routing

**Foundation:** workflow_definition + steps (value / commodity / plant routing).

**Proposed:** admin UX for CRUD definitions, step sequence, approver roles, activate/deactivate.  
**Rules:** no delete with in-progress instances; no “AI auto-approve” step; override remains justified human path.

### 4.5 Costing defaults

| Default key | Constraint |
|-------------|------------|
| `default_reporting_currency` | Must exist in currency master |
| `default_scrap_percent` | ≥ 0; document may override |
| `default_overhead_percent` | nullable; document may override |
| `fx_missing_policy` | Fixed `NOT_AVAILABLE` in v1 (not customer-settable to assume 1.0) |
| `release_incomplete_policy` | Recommend keep `BLOCK` (P3-01 §12) — not customer-editable in v1 |

Changing defaults does **not** rewrite historical build-ups.

---

## 5. Config object lifecycle

```text
DRAFT → PUBLISHED → RETIRED
         ↑
    new version = clone to DRAFT
```

Publish and retire are audited (user, tenant, before/after).

---

## 6. Proposed API (design only)

Prefix: `/api/v1/configuration/` (tenant-scoped)

| Method | Path | Purpose |
|--------|------|---------|
| GET/PUT | `/module-packs` / entitlements | Packs + enable within contract |
| GET/POST | `/scorecard-weight-sets` | List/create DRAFT |
| POST | `/scorecard-weight-sets/{id}/publish` | Validate 100% + publish |
| GET/POST | `/questionnaire-packs` | Pack CRUD |
| POST | `/questionnaire-packs/{id}/publish` | Freeze items |
| GET/PUT | `/workflow-definitions` | Admin on existing model |
| GET/PUT | `/costing-defaults` | Tenant defaults |
| GET | `/effective` | Resolved read model |

---

## 7. Proposed UI

| Area | Route (proposed) |
|------|------------------|
| Configuration home | `/platform/configuration` or `/configuration` |
| Modules / packs | tab |
| Scorecard weights | tab + version timeline |
| Questionnaire packs | tab |
| Workflow | existing admin or embed |
| Costing defaults | tab |
| Config audit | filter audit log |

Visible to Customer Admin / Platform Admin only.

---

## 8. Evaluation binding

```text
PUBLISHED weight set v3 + PUBLISHED questionnaire pack v2
        ↓
   Evaluation run stores version ids
        ↓
   Later v4 does NOT change historical scores
```

Aligns with Master Spec question versioning (§21).

---

## 9. Phased implementation (if later authorized)

| Phase | Scope | Rationale |
|-------|--------|-----------|
| **P3-07a** | Scorecard weight sets + bind to evaluation | Highest VS value; engine ready |
| **P3-07b** | Questionnaire packs admin UX | Bank exists |
| **P3-07c** | Costing defaults | Small; high rollout value |
| **P3-07d** | Module packs UX | Entitlements exist |
| **P3-07e** | Workflow admin polish | Definitions exist |

---

## 10. Testing (when implementation authorized)

- Weights ≠ 100% → publish rejected  
- Historical evaluation keeps old weight version  
- Tenant isolation  
- Non-admin cannot publish  
- Default change does not alter frozen build-ups  

---

## 11. Open decisions — **LOCKED 2026-08-15**

| # | Answer |
|---|--------|
| 1 | **PLATFORM** |
| 2 | **system default** |
| 3 | **forbid** last published retire |
| 4 | **No** |
| 5 | **No** |
| 6 | **defer** |
| 7 | **7a first** |

### Original options (archive)


| # | Decision | Options / recommendation |
|---|----------|---------------------------|
| 1 | UI under PLATFORM only or new `CONFIGURATION` module? | Either |
| 2 | System-default scorecard if tenant has none? | Recommend system default |
| 3 | Retire last PUBLISHED weight set? | Recommend forbid |
| 4 | Pack may exclude mandatory-gate questions? | Recommend **No** |
| 5 | Customer edits `release_incomplete_policy`? | Recommend **No** (keep BLOCK) |
| 6 | Cross-tenant export/import? | Defer |
| 7 | Phase order | Recommend **7a → 7b → 7c → 7d → 7e** |

---

## 12. Approval gate

```text
DESIGN ONLY (this document)
        ↓
Product answers §11 + APPROVE this feature
        ↓
AUTHORIZE P3-07 IMPLEMENTATION
Scope: e.g. P3-07a only
        ↓
MASTER-Vxxx with schema/API/FE/tests
```

**No runtime code for P3-07 under DESIGN ONLY.**

---

## 13. Traceability to existing assets

| Asset | Role |
|-------|------|
| Central Access entitlements | Module packs foundation |
| `ScorecardScoringEngine` | Weight math |
| `vendor_question` bank | Questionnaire items |
| Workflow definitions | Routing |
| Currency module | Default currency validation |
| Audit log | Publish trail |

---

*End of PROPOSED REQUIREMENT — P3-07 Customer Configuration Center.*
