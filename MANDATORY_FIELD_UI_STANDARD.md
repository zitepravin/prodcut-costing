# Mandatory Field UI Standard

**Package:** MASTER V202  
**Scope:** Entire Product Costing frontend

## Rules

1. Every **business-mandatory** field shows a **red asterisk (*)** after its label.
2. Asterisk color: theme `error.main` (`#c62828`), bold — applied via `MuiFormLabel` / `MuiInputLabel` asterisk overrides in `theme/appTheme.ts`.
3. Optional fields **must not** use the `required` prop or show `*`.
4. Forms show the legend: **\* Required field** via `<RequiredFieldLegend />` (`components/ux/RequiredFieldLegend.tsx`).
5. Do **not** mark a field mandatory only because it “looks important” — align with client validation and backend contracts.
6. Do **not** change validation logic solely to display the asterisk.
7. Accessibility: MUI `required` sets `aria-required` on inputs; asterisk is not the only cue (legend text + required state).

## Implementation

| Mechanism | Role |
|-----------|------|
| `required` on TextField / Select / FormControl | Shows red * and aria-required |
| `RequiredFieldLegend` | Form-level “* Required field” note |
| Theme asterisk styles | Consistent red * across modules |

## Module audit (V202)

| Module | Mandatory indicators |
|--------|----------------------|
| Login | Username, Password + legend |
| Materials create | Code, Rate, UOM, Density, Effective from + legend |
| Should-Cost | BOM node, Commodity (full); parametric families + legend |
| Tooling create | BOM node, Description, Type, Tool cost + legend |
| Process costing | BOM node ID + legend |
| Packaging | BOM node ID load + legend |
| Target costing | BOM node; target amount; dialog legend |
| RFQ | Existing required props + legend |
| ERP staging | Existing required + legend |
| Currency convert | From / To / Amount where applicable + legend |
| Vendor Selection | Supplier ID + page legend |
| Platform config | customer field where required |

## Developer checklist

When adding a form field:

1. Confirm backend / client validation requires it.
2. If yes → `required` prop on the control.
3. If the form has any required fields → include `<RequiredFieldLegend />`.
4. If optional → no `required`, no asterisk.
