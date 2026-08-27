# Frontend stack — Phase 1 decision

**Status:** Locked for Phase 1  
**Date:** 2026-08-12

## Chosen stack

| Layer | Version family | Notes |
|-------|----------------|-------|
| Runtime | React 19.x | Matches `@types/react` 19 |
| UI | **MUI 6.5.x** (`~6.5.0`) | Not MUI 7/9 |
| Styling | Emotion 11.x | Required MUI peer |
| Routing | React Router 7.x | Node ≥ 20 |
| HTTP | Axios 1.19.x | JWT attach + refresh |
| Bundler | Vite 8.x + `@vitejs/plugin-react` 6.x | Node ≥ 20.19 or ≥ 22.12 |
| Language | TypeScript ~6.0 | `tsc -b` in CI/build |
| Lint | oxlint | Fast refresh–friendly |

Node engine: **≥ 20.19.0**

## Why MUI stays on 6

npm latest is MUI **9.x**. Phase 1 deliberately stays on **6.5.x** because:

1. MUI 9 removes/relocates system props (`fontWeight`, `alignItems`, `textAlign`, etc.) to `sx` only — large, non-functional churn across every screen.
2. Grid API migration (classic `item`/`xs` → Grid2) is required for a clean 9 upgrade.
3. Current tree builds and runs with zero peer conflicts on MUI 6 + React 19.

**Do not bump `@mui/material` or `@mui/icons-material` to 7+ without a planned migration PR.**

## Out of scope until planned

- MUI 7 / 9 upgrade
- TypeScript 7
- Optional `@mui/material-pigment-css`
- Route-level code splitting (bundle size optimization)

## Verification

```bash
npm install
npm run build   # must stay green on this stack
```

---

## Package release alignment

| Frontend package version | Backend Flyway (max) | Artifact |
|--------------------------|----------------------|----------|
| 0.87.0 | V87 | product-costing-frontend-v87.zip |

Update `package.json` version when cutting a new platform zip so UI and API contract stay traceable.
