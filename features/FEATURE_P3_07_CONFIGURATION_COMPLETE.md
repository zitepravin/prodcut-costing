# FEATURE P3-07 — Configuration Center (complete related slices)

**Package:** MASTER-V185  
**Design:** APPROVED with §11 locked  
**Slices:** 7a (V184) + **7b / 7c / 7d / 7e (V185)**

## Delivered

| Slice | Content |
|-------|---------|
| **7a** | Scorecard weight publish / system default / last ACTIVE guard (V184) |
| **7b** | Questionnaire packs DRAFT→ACTIVE; must include active mandatory-gate questions |
| **7c** | Tenant costing defaults; FX=NOT_AVAILABLE & RELEASE incomplete=BLOCK fixed |
| **7d** | Module pack catalog (COSTING_ONLY / COSTING_PLUS_VS / FULL_SOURCING) |
| **7e** | Workflow definitions read model in config center |

## Schema
Flyway **V102** — `questionnaire_pack`, `questionnaire_pack_item`, `tenant_costing_defaults`

## APIs
`/api/v1/configuration/*` — effective, module-packs, questionnaire-packs, costing-defaults, workflow-definitions

## UI
`/platform/configuration` — tabs for all slices

## Policy
No AI publish/approve · no invent FX · no SAP MM · no customer override of BLOCK/NOT_AVAILABLE policies
