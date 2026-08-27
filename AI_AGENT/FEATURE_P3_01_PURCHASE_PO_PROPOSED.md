# FEATURE P3-01 — Purchase / Purchase Order (PROPOSED REQUIREMENT)

**Document type:** PROPOSED REQUIREMENT  
**Gap ID:** P3-01  
**Version target (if approved):** MASTER-V176+  
**Date:** 2026-08-15  
**Status:** **APPROVED FOR IMPLEMENTATION (Option D) — MASTER-V177**  
**Authorization path:** Design-only (Option A). Implementation (Option D) only after product confirms this document.

---

## 1. Business purpose

After a human **Make-vs-Buy decision** of `APPROVE_BUY` or `APPROVE_MIXED`, and after a supplier is **APPROVED** in Vendor Selection for the relevant part/BOM context, the organization needs a controlled way to record a **purchase intent / purchase order draft** inside this platform — without replacing the customer ERP.

This module answers:

> Which approved supplier and commercial terms should be used for the BUY portion of this part, and what is the auditable purchase record before ERP/PO systems take over?

It does **not** answer ERP inventory, goods receipt, or invoice matching.

---

## 2. Explicit non-goals (frozen even if P3-01 is approved)

| Non-goal | Reason |
|----------|--------|
| Automatic PO creation from AI recommendation | Master Spec §26–§31 — human decision mandatory |
| Automatic supplier award / approval | Human approval remains mandatory |
| Live post to SAP MM / ME21N from this app | CPI/ERP owns execution; we stay decision + export |
| Full Order Tracking / ASN / GR / IR | Separate future module (not P3-01) |
| Material Ledger / actual costing | Out of product scope |
| Invented unit price or FX | Missing data → `DATA NOT AVAILABLE` |

**AI agent role:** help / explain / draft only. **Never** create, release, or approve a PO.

---

## 3. Preconditions (mandatory gates before any PO draft)

All must pass; numerical score does not override:

1. Tenant has module entitlement `PURCHASE` enabled (`platform_module.module_code = 'PURCHASE'` already seeded in V32).
2. User has program/BOM scope access.
3. Linked **Make-vs-Buy simulation** status = `DECIDED` and decisionCode ∈ {`APPROVE_BUY`, `APPROVE_MIXED`}.
4. Preferred BUY leg has:
   - `eligibilityStatus` ≠ `NOT_ELIGIBLE`
   - Supplier lifecycle **not** `BLOCKED` / `SUSPENDED`
5. Supplier has **APPROVED** qualification lifecycle for the applicable context (same human-approval gate as Vendor Selection).
6. Commercial source is an **ACCEPTED** supplier quote **or** explicit human override reason (override is audited).
7. Currency / FX: if quote currency ≠ PO reporting currency and no rate exists → PO line `dataStatus = NOT_AVAILABLE` (no invented rate).

---

## 4. Proposed domain model

### 4.1 Purchase request (header)

| Field | Type | Notes |
|-------|------|-------|
| purchase_request_id | CHAR(36) PK | UUID |
| tenant / customer_id | CHAR(36) | Multi-tenant isolation |
| program_id | CHAR(36) | Scope |
| bom_node_id | CHAR(36) | Part context |
| make_vs_buy_simulation_id | CHAR(36) NULL | Preferred source link |
| vendor_selection_decision_id | CHAR(36) NULL | Optional VS link |
| status | VARCHAR(20) | See lifecycle |
| reporting_currency | CHAR(3) | Default tenant base |
| requested_by / requested_at | audit | |
| approved_by / approved_at | audit NULL | Human only |
| rejection_reason | TEXT NULL | |
| notes | TEXT NULL | |

### 4.2 Purchase request line

| Field | Type | Notes |
|-------|------|-------|
| line_id | CHAR(36) PK | |
| purchase_request_id | CHAR(36) FK | |
| line_no | INT | |
| supplier_id | CHAR(36) | |
| supplier_quote_id | CHAR(36) NULL | ACCEPTED quote preferred |
| quantity | DECIMAL(18,6) | BigDecimal |
| uom | VARCHAR(20) | |
| unit_price | DECIMAL(18,6) NULL | Null if NOT_AVAILABLE |
| currency | CHAR(3) | Quote currency |
| unit_price_reporting | DECIMAL(18,6) NULL | After FX; null if no rate |
| fx_rate | DECIMAL(18,8) NULL | |
| data_status | VARCHAR(20) | AVAILABLE / NOT_AVAILABLE |
| tco_unit | DECIMAL(18,6) NULL | Snapshot if VS TCO complete |
| tco_status | VARCHAR(20) | AVAILABLE / NOT_AVAILABLE |
| need_by_date | DATE NULL | |
| line_status | VARCHAR(20) | mirrors header constraints |

### 4.3 Lifecycle (proposed)

```text
DRAFT
  → SUBMITTED          (requester)
  → APPROVAL_PENDING   (workflow)
  → APPROVED           (human approver only)
  → RELEASED           (export package generated / marked ready for ERP)
  → CANCELLED
  → REJECTED
```

Negative / terminal: `REJECTED`, `CANCELLED`.

**No AI path may set APPROVED or RELEASED.**

### 4.4 Export package (read-only)

Analogous to Make-vs-Buy P3-04:

`GET /api/v1/purchase-requests/{id}/export-package`

- JSON for CPI / process teams  
- **Does not** post to SAP MM  
- Includes decision trail: MvB simulation id, VS approval refs, quote ids, FX status, approver, timestamps  

---

## 5. Proposed API surface (draft)

All under `@RequiresModule("PURCHASE")` + tenant + program scope.

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/v1/bom-nodes/{bomNodeId}/purchase-requests` | Create DRAFT from DECIDED MvB (or manual with reason) |
| GET | `/api/v1/purchase-requests/{id}` | Get header + lines |
| GET | `/api/v1/programs/{programId}/purchase-requests` | List |
| POST | `/api/v1/purchase-requests/{id}/submit` | DRAFT → SUBMITTED |
| POST | `/api/v1/purchase-requests/{id}/decide` | Human APPROVE / REJECT + reason |
| POST | `/api/v1/purchase-requests/{id}/release` | APPROVED → RELEASED (export ready) |
| GET | `/api/v1/purchase-requests/{id}/export-package` | Read-only JSON |

**Validation examples:**

- Create without DECIDED MvB → require `manualOverrideReason`  
- Decide without role/workflow permission → 403  
- Release when any line `dataStatus=NOT_AVAILABLE` → block **or** allow with explicit `acceptIncompleteData` flag + audit (product must choose; default propose = **block**)  

---

## 6. Integration with existing modules

```text
Vendor Selection (APPROVED supplier)
        +
Make-vs-Buy (DECIDED APPROVE_BUY / APPROVE_MIXED)
        +
ACCEPTED quote / TCO snapshot when complete
        ↓
Purchase Request DRAFT (P3-01)
        ↓
Human submit / approve / release
        ↓
Export package → customer CPI / ERP
```

- **Product Costing** remains source of should-cost / variance context (display only on PR UI).  
- **Central Access:** module `PURCHASE` already exists; default entitlement remains **off** until customer enables.  
- **Workflow / notifications:** reuse existing workflow engine patterns for approval tasks.  
- **Audit:** previous/new status, actor, reason, linked simulation/quote ids.

---

## 7. Frontend (proposed)

- Route: `/purchase-requests` (nav under Operations or Purchasing, entitlement-gated).  
- Actions: Create from Make-vs-Buy (only when DECIDED BUY/MIXED), list, detail, submit, decide, release, **Export package**.  
- Display: quote vs should-cost vs TCO when available; otherwise `DATA NOT AVAILABLE`.  
- Helping Agent: may **explain** PR status and linked evidence; may **not** approve or release.

---

## 8. Financial rules

- All money fields: `BigDecimal` only.  
- Precision/scale: align with existing quote / should-cost conventions.  
- FX: same service as Make-vs-Buy / Mixed (CurrencyService); missing rate → `NOT_AVAILABLE`, never invent.  
- TCO on line: snapshot only when VS TCO `dataComplete`; else `tco_status=NOT_AVAILABLE`.

---

## 9. Security

- Backend enforcement of tenant isolation and `@RequiresModule("PURCHASE")`.  
- Frontend hide is not security.  
- APPROVED / RELEASED transitions require authenticated human with approval permission (reuse RBAC patterns).  
- BLOCKED/SUSPENDED supplier cannot be on an APPROVED PR.

---

## 10. Testing requirements (when implementation authorized)

| Type | Cases |
|------|--------|
| Unit | Create from DECIDED MvB; reject create from DRAFT MvB; FX missing → NOT_AVAILABLE |
| Negative | Wrong tenant; module disabled; BLOCKED supplier; AI path cannot approve |
| Calculation | unit_price_reporting = unit_price × fx when rate exists |
| Integration | Workflow submit → approve → export package shape |
| FE | Entitlement gate; Export package download |

---

## 11. Database impact (if approved)

- New Flyway version **V97+** (after current V96).  
- Tables: `purchase_request`, `purchase_request_line` (+ optional audit/event outbox if required by workflow pattern).  
- No breaking changes to existing MvB / VS / quote tables — additive FKs only.

---

## 12. Open decisions for product (must answer before Option D)

1. **Incomplete commercial data:** Block RELEASE when any line is `NOT_AVAILABLE`, or allow with forced acknowledgment?  
   **Proposal:** Block by default.  
2. **Manual PR without MvB:** Allowed with mandatory reason, or MvB-only?  
   **Proposal:** Allowed with mandatory `manualOverrideReason`.  
3. **Quantity source:** Annual volume from BOM/program curve vs user-entered PO qty?  
   **Proposal:** User-entered qty; default suggestion from program volume when available.  
4. **Multi-line / multi-supplier mixed BUY:** One PR with multiple lines, or one PR per supplier?  
   **Proposal:** One PR header, multiple lines (supports APPROVE_MIXED).  
5. **ERP document number:** Store external PO number after CPI callback, or out of scope for P3-01?  
   **Proposal:** Optional `external_po_reference` field; no inbound required for MVP.

---

## 13. Implementation readiness

| Item | State |
|------|--------|
| Module code `PURCHASE` in platform_module | Exists (V32) |
| MvB DECIDED + export pattern | Exists (V168–V175) |
| VS APPROVED human gate | Exists |
| Quote ACCEPTED | Exists |
| FX / NOT_AVAILABLE pattern | Exists (V174) |
| SRS/TDD for full PO | **Missing — this document is the first PROPOSED REQUIREMENT** |
| Code / Flyway / FE for P3-01 | **Not started** |

---

## 14. Recommendation to product

1. Review and answer **§12 Open decisions**.  
2. Mark this document **APPROVED** or return with changes.  
3. Only then authorize **Option D** implementation under Definition of Done.  
4. Keep Helping AI Agent **help-only** (no intervene) for all PR transitions.

---

**PROPOSED REQUIREMENT — Product approved Option D. Implemented in MASTER-V177 with §12 defaults: block RELEASE on NOT_AVAILABLE; manual PR with reason; user qty; multi-line; optional external_po_reference.**
