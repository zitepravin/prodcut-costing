# A6 — Overhead / Costing Sheet Engine

**Package:** MASTER V211

## Model
- `costing_sheet` — code, name, plant, currency, validity, DRAFT→ACTIVE→RETIRED
- `costing_sheet_rule` — sequenced rules:
  - **PERCENT_OF_BASE** on MATERIAL | PROCESS | TOOLING | PACKAGING | SUBTOTAL | TOTAL_SO_FAR
  - **FIXED_AMOUNT**
  - Maps to `component_code` + cost_behavior

## Engine
`CostingSheetEngine.apply(base, rules)` — pure BigDecimal sequential application with audit lines.

Worklist example (both OH on SUBTOTAL):
Material 100 + Process 35 + 10% FOH + 3% Admin = **152.55**

## API
- `GET/POST /api/v1/costing-sheets`
- `POST /api/v1/costing-sheets/{id}/rules`
- `POST .../activate` | `.../retire`
- `POST /api/v1/costing-sheets/apply` — audited overhead result

## UI
Product Costing → Costing Sheets (`/costing-sheets`)

## Non-goals
Does not replace overhead pools; sheets are the sequential rule engine. Optional link via variant `costingSheetCode` remains configuration-level.
