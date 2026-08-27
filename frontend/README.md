# Product Costing + Vendor Selection — Frontend

**Package:** `product-costing-frontend` **0.87.0**  
**Stack:** React 19 · TypeScript ~6 · Material UI **6.5** · Vite 8 · Axios · React Router 7  
**Backend alignment:** `product-costing-backend` Flyway **V87** (next free **V88**)

Enterprise UI for automobile / manufacturing product costing, should-cost, TCO, supplier discovery through approval, evidence verification, and compliance audit.

Phase 1 UI stack is **locked**. See `FRONTEND_STACK.md`. Do not upgrade MUI to 7/9 without a migration plan.

---

## Package details

| Field | Value |
|-------|--------|
| npm name | `product-costing-frontend` |
| version | `0.87.0` (tracks platform release V87) |
| private | `true` (not published to npm registry) |
| license | UNLICENSED |
| module type | ES module (`"type": "module"`) |
| Node engine | `>= 20.19.0` |
| npm engine | `>= 10` |
| Build output | `dist/` (Vite production bundle) |

### Scripts

| Script | Command | Purpose |
|--------|---------|---------|
| `dev` | `npx vite` | Local dev server (default http://localhost:5173) |
| `build` | `npx tsc -b && npx vite build` | Typecheck + production build |
| `typecheck` | `npx tsc -b --pretty false` | TypeScript only |
| `lint` | `oxlint` | Fast lint |
| `preview` | `npx vite preview` | Serve production build locally |

### Runtime dependencies

| Package | Range | Role |
|---------|-------|------|
| `react` / `react-dom` | ^19.2.x | UI runtime |
| `@mui/material` / `@mui/icons-material` | ~6.5.0 | Component library (pinned) |
| `@emotion/react` / `@emotion/styled` | ^11.14.x | MUI styling peers |
| `react-router-dom` | ^7.x | App routing |
| `axios` | ^1.19.x | HTTP + JWT attach/refresh |

### Dev dependencies

| Package | Role |
|---------|------|
| `typescript` ~6.0 | Typecheck |
| `vite` ^8 + `@vitejs/plugin-react` | Bundler |
| `@types/react`, `@types/react-dom`, `@types/node` | Types |
| `oxlint` | Lint |

---

## Setup

```bash
cp .env.example .env
# VITE_API_BASE_URL=http://localhost:8080

npm install
npm run dev
```

Open http://localhost:5173  

Backend must be running (MySQL + Flyway through **V87** applied). Sign in with a provisioned user (seed `admin` after platform migrations).

---

## Implemented screens

| Route | Screen | Notes |
|-------|--------|--------|
| `/login` | Login | JWT login / refresh / logout |
| `/` | Dashboard | KPIs including vendor queue, expiring capabilities |
| `/materials` | Materials | Master material search |
| `/bom` | BOM | Product structure |
| `/should-cost` | Should-cost | Bottom-up cost |
| `/approvals` | Approvals | Workflow steps |
| `/notifications` | Notifications | In-app notifications |
| `/machines` | Machines | Machine rates |
| `/process` | Process costing | Operations / process cost |
| `/tooling` | Tooling | Tooling cost |
| `/packaging` | Packaging / logistics | Profiles |
| `/target-costing` | Target costing | Targets / variance |
| `/rfq` | RFQ / suppliers | Supplier master / RFQ |
| `/vendor-selection` | **Vendor selection** | 360, eligibility, TCO, scorecard, comparison, recommendation, qualification, risk, config, evidence filters |
| `/cost-comparison` | Cost comparison | Quote vs should-cost |
| `/import-export` | Import / export | Data exchange |
| `/currency` | Currency | Rates / convert |
| `/audit` | **Audit log** | CREATE / UPDATE / TRANSITION / VERIFY / DELETE filters (admin) |
| `/erp-staging` | ERP staging | Inbound batches |
| `/versions` | Version control | Snapshots |
| `/reports` | Reports | Operational reports |

### API client modules (`src/api/`)

`authApi`, `dashboardApi`, `materialApi`, `bomApi`, `shouldCostApi`, `workflowApi`, `notificationApi`, `machineApi`, `processApi`, `toolingApi`, `packagingApi`, `targetCostingApi`, `rfqApi`, **`vendorSelectionApi`**, `costComparisonApi`, `importExportApi`, `currencyApi`, **`auditApi`**, `erpStagingApi`, `versionApi`, `reportsApi`, `httpClient`.

---

## Conventions

- Money via `formatMoney` / `formatRate` from decimal **strings** — no `parseFloat` arithmetic.
- Tokens in `localStorage` (`pcs.*`); access token refreshed proactively and on 401.
- Role checks via JWT `authorities` (UI gating only; server enforces `@PreAuthorize`).
- Vendor selection: no auto-approve; evidence ≠ commercial claim status; audit actions TRANSITION / VERIFY.

---

## Release package

Artifact zip: `product-costing-frontend-v87.zip` (pair with `product-costing-backend-v87.zip`).

```bash
npm ci
npm run build   # must stay green
```
