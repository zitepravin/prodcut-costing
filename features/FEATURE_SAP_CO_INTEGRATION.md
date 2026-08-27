# FEATURE_SAP_CO_INTEGRATION.md

**Feature:** SAP CO cost estimate inbound + compare to should-cost  
**Version:** MASTER-V166  
**Module:** Product Costing / ERP  
**Status:** Implemented (backend contract-first v1)  
**Date:** 2026-08-15

## Purpose
Land SAP Product Cost Planning estimates (via customer CPI) and compare to our should-cost without inventing missing data. No live RFC from multi-tenant runtime.

## What it does
- POST land cost estimate + components
- Map SAP component codes → our codes (P2-01 style)
- Compare totals and components to a should-cost build-up
- Explicit DATA NOT AVAILABLE / PARTIAL

## What it does not do
- Call SAP BAPI/RFC directly
- Post estimates into SAP CO
- Actual Costing / Material Ledger

## APIs
| Method | Path |
|--------|------|
| POST | /api/v1/erp/sap/cost-estimates |
| GET | /api/v1/erp/sap/cost-estimates |
| GET | /api/v1/erp/sap/cost-estimates/{id} |
| POST | /api/v1/erp/sap/cost-estimates/{id}/map-components |
| POST | /api/v1/erp/sap/cost-estimates/{id}/compare-should-cost |
| GET/PUT | /api/v1/erp/sap/component-mappings |

## Database
Flyway **V95**: sap_cost_estimate_staging, sap_cost_estimate_component_staging, sap_component_mapping

## Security
@RequiresModule("PRODUCT_COSTING")

## Tests
SapShouldCostCompareEngineTest — 4 cases written (Maven not run on packaging agent)

## Customer path
SAP BAPI → Customer CPI → POST /api/v1/erp/sap/cost-estimates
