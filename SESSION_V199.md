# SESSION V199 — Mandatory field star (platform-wide)

**Date:** 2026-08-16  
**Baseline:** V198

## Change
1. **Theme** (`frontend/src/theme/appTheme.ts`): required FormLabel / InputLabel asterisk is **red** (`#c62828`), bold — applies to every screen that uses MUI `required`.
2. **Rule:** Mandatory inputs must use the `required` prop (not only text like "(required)" in the label).
3. **Aligned fields:** Purchase Request BOM Node ID; correction reason; on-time override reason; Make-vs-Buy decision reason.

## Verify
```
docker compose build frontend --no-cache
docker compose up -d frontend
```
Open Login, Materials create, Should-Cost generate, Make-vs-Buy decide, Purchase Requests — required labels show **red ***.

## DO NOT
Invent FX / AI approve / SAP MM post / RELEASE on NOT_AVAILABLE without product change
