# SESSION V198 — Global Back to Dashboard on every screen

**Date:** 2026-08-16  
**Type:** UX (customer testing feedback)

## Request
Back button must appear on **every** page, not only Cost Comparison.

## Fix
`frontend/src/layout/AppLayout.tsx`:
- When `location.pathname !== '/'`, render **Back to Dashboard** (outlined button) above `<Outlet />`
- Uses existing `navigate('/')` — works for all module pages under AppLayout
- Hidden on Dashboard itself (home)

Cost Comparison keeps breadcrumbs only (no duplicate page-level back).

## Unchanged
Policies: no AI approve, no invented FX, no SAP MM post, RELEASE blocked on NOT_AVAILABLE.
