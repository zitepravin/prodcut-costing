# Global Module Navigation Hierarchy

**Package:** MASTER V205

## Root cause

1. AppLayout previously used universal **Back to Dashboard**.
2. V203 hid Back on top-level modules but breadcrumbs were only `Dashboard > Page`.
3. Sidebar **group labels** (Master data, Product costing, …) were not routes — no intermediate landing.
4. Some pages (SAP, Make vs Buy, …) added **duplicate** page-level breadcrumbs.

## Architecture

```text
MODULES[] in routeHierarchy.ts  (single source of truth)
        ↓
resolveRouteNavMeta(pathname)
        ↓
BackToParentBar (shell) → breadcrumbs + Back to module landing
        ↓
ModuleLandingPage at /master-data | /product-costing | /sourcing | /operations
```

## Hierarchy

| Module landing | Children |
|----------------|----------|
| `/master-data` | Materials, Machines, Currency, Import / Export |
| `/product-costing` | BOM, Process, Tooling, Packaging, Should-Cost, Lifecycle, Mixed Costing, Make vs Buy, Purchase Requests, Target Costing, Cost Comparison, Versions |
| `/sourcing` | RFQ / Quotes, Vendor Selection |
| `/operations` | Reports, ERP Staging, SAP Cost Estimates, Audit Log |

## Examples

| Page | Breadcrumb | Back |
|------|------------|------|
| Make vs Buy | Dashboard > Product Costing > Make vs Buy | Back to Product Costing |
| Currency | Dashboard > Master Data > Currency | Back to Master Data |
| Vendor Selection | Dashboard > Sourcing > Vendor Selection | Back to Sourcing |
| Reports | Dashboard > Operations > Reports | Back to Operations |
| Product Costing landing | Dashboard > Product Costing | Back to Dashboard |

## Files

- `navigation/routeHierarchy.ts` — MODULES tree + resolveRouteNavMeta
- `navigation/BackToParentBar.tsx` — shell control
- `pages/ModuleLandingPage.tsx` — intermediate landings
- `App.tsx` — routes for landings
- `layout/AppLayout.tsx` — nav items for landings
- Page-level duplicate breadcrumbs removed from listed pages

## Tests

- Vitest routeHierarchy: 8 cases
- Playwright e2e/navigation-hierarchy.spec.ts (needs live auth)
