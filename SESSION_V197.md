# SESSION V197 — Cost Comparison: Back to Dashboard

**Date:** 2026-08-16  
**Type:** UX defect fix (customer dashboard testing)

## Issue
Cost Comparison page had no clear control to return to Dashboard. Sidebar works, but users expected an on-page Back button.

## Fix
`frontend/src/pages/CostComparisonPage.tsx`:
- Use shared `PageHeader` with breadcrumbs: Dashboard → Product costing → Cost Comparison
- Explicit **Back to Dashboard** outlined button (`RouterLink` to `/`)

## Unchanged
No API / schema / policy changes. AI help-only still in force.
