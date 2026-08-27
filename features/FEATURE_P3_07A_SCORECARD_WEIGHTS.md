# FEATURE P3-07a — Versioned Scorecard Weight Sets

**Package:** MASTER-V184  
**Parent:** P3-07 Configuration Center (design APPROVED 2026-08-15)

## §11 decisions applied
1. UI under **PLATFORM** (`/platform/configuration`)
2. **System default** when evaluation omits template id (`VS-DEFAULT-100`, `is_system_default=1`)
3. **Forbid** retiring last ACTIVE/published template
4–6. N/A for 7a / deferred
7. **7a first**

## Delivered
- Flyway **V101** `is_system_default` on `vendor_scorecard_template`
- Status model: DRAFT | ACTIVE (published) | INACTIVE (retired)
- `publishTemplate`, hardened `deactivateTemplate`, `resolveEffectiveTemplate`
- Evaluate uses system default when templateId blank
- APIs: POST `.../publish`, GET `.../effective`
- FE: Platform Configuration page (list / publish / retire)

## Policy
- Weights must total 100% on publish/create
- Historical results remain bound to template id
- No AI publish / auto-approve
