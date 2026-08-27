# TABLE_TENANT_CLASSIFICATION_V272 (authoritative — MASTER-V273)
**Rule:** Every table must be classified. Unknown = production blocker.
**Source:** Flyway CREATE TABLE inventory (153 tables).

| Table | Classification | customer_id status | Notes |
|-------|----------------|--------------------|-------|
| `access_scope` | PLATFORM | via customer FK where applicable | Control plane |
| `activity_type` | GLOBAL_REFERENCE | N/A | Shared reference |
| `actual_cost_record` | TENANT_OWNED | required | Business data |
| `additive_cost_line` | TENANT_OWNED | required | Business data |
| `app_notification` | TENANT_OWNED | required | Business data |
| `app_user` | PLATFORM | via customer FK where applicable | Control plane |
| `approval_step_instance` | TENANT_OWNED | required | Business data |
| `approval_workflow_instance` | TENANT_OWNED | required | Business data |
| `audit_log` | AUDIT | optional / recommended | Security audit |
| `benchmark_pack` | GLOBAL_REFERENCE | N/A or tenant override later | Market data |
| `benchmark_rate` | GLOBAL_REFERENCE | N/A or tenant override later | Market data |
| `billing_chargeback` | PLATFORM | via customer FK where applicable | Control plane |
| `billing_invoice` | PLATFORM | via customer FK where applicable | Control plane |
| `billing_payment` | PLATFORM | via customer FK where applicable | Control plane |
| `billing_plan` | GLOBAL_REFERENCE | N/A | Shared reference |
| `billing_plan_module` | GLOBAL_REFERENCE | N/A | Shared reference |
| `billing_refund` | PLATFORM | via customer FK where applicable | Control plane |
| `billing_webhook_event` | PLATFORM | via customer FK where applicable | Control plane |
| `bom_node` | TENANT_OWNED | required | Business data |
| `business_case_scenario` | TENANT_OWNED | required | Business data |
| `capability_expiry_job_status` | SYSTEM_JOB | nullable routing customer_id on outbox | Workers |
| `capability_expiry_run` | SYSTEM_JOB | nullable routing customer_id on outbox | Workers |
| `capacity_center` | TENANT_OWNED | required | Business data |
| `capacity_load` | TENANT_OWNED | required | Business data |
| `commodity_index_value` | GLOBAL_REFERENCE | N/A or tenant override later | Market data |
| `confidentiality_wall` | TENANT_OWNED | required | Business data |
| `cost_component_definition` | GLOBAL_REFERENCE | N/A | Shared reference |
| `cost_estimate_item` | TENANT_OWNED | required | Business data |
| `cost_version_record` | TENANT_OWNED | required | Business data |
| `costing_run` | TENANT_OWNED | required | Business data |
| `costing_run_item` | TENANT_OWNED | required | Business data |
| `costing_scenario` | TENANT_OWNED | required | Business data |
| `costing_sheet` | TENANT_OWNED | required | Business data |
| `costing_sheet_rule` | TENANT_OWNED | required | Business data |
| `costing_variant` | TENANT_OWNED | required | Business data |
| `currency` | GLOBAL_REFERENCE | N/A | Shared reference |
| `customer_department` | PLATFORM | via customer FK where applicable | Control plane |
| `customer_module_entitlement` | PLATFORM | via customer FK where applicable | Control plane |
| `customer_subscription` | PLATFORM | via customer FK where applicable | Control plane |
| `customer_user_membership` | PLATFORM | via customer FK where applicable | Control plane |
| `domain_outbox` | SYSTEM_JOB | nullable routing customer_id on outbox | Workers |
| `ecn_cost_impact` | TENANT_OWNED | required | Business data |
| `emission_factor` | GLOBAL_REFERENCE | N/A or tenant override later | Market data |
| `erp_bom_staging` | TENANT_OWNED | batch has customer_id (V142); children via batch | Integration staging |
| `erp_inbound_batch` | TENANT_OWNED | batch has customer_id (V142); children via batch | Integration staging |
| `erp_material_price_staging` | TENANT_OWNED | batch has customer_id (V142); children via batch | Integration staging |
| `erp_part_staging` | TENANT_OWNED | batch has customer_id (V142); children via batch | Integration staging |
| `evidence_expiry_job_status` | SYSTEM_JOB | nullable routing customer_id on outbox | Workers |
| `evidence_expiry_run` | SYSTEM_JOB | nullable routing customer_id on outbox | Workers |
| `exchange_rate` | GLOBAL_REFERENCE | N/A or tenant override later | Market data |
| `labor_master` | TENANT_OWNED | required | Business data |
| `machine_master` | TENANT_OWNED | required | Business data |
| `make_vs_buy_leg` | TENANT_OWNED | required | Business data |
| `make_vs_buy_simulation` | TENANT_OWNED | required | Business data |
| `material_cost_calculation` | TENANT_OWNED | required | Business data |
| `material_master` | TENANT_OWNED | required | Business data |
| `mixed_cost_leg` | TENANT_OWNED | required | Business data |
| `mixed_cost_version` | TENANT_OWNED | required | Business data |
| `notification_delivery` | TENANT_OWNED | required | Business data |
| `notification_preference` | TENANT_OWNED | required | Business data |
| `notification_template` | GLOBAL_REFERENCE | N/A | Shared reference |
| `oidc_identity_link` | PLATFORM | via customer FK where applicable | Control plane |
| `overhead_pool` | TENANT_OWNED | required | Business data |
| `packaging_logistics_cost_calculation` | TENANT_OWNED | required | Business data |
| `packaging_logistics_profile` | TENANT_OWNED | required | Business data |
| `parametric_cost_benchmark` | GLOBAL_REFERENCE | N/A or tenant override later | Market data |
| `plant_master` | TENANT_OWNED | required | Business data |
| `platform_customer` | PLATFORM | via customer FK where applicable | Control plane |
| `platform_module` | GLOBAL_REFERENCE | N/A | Shared reference |
| `portal_event_log` | AUDIT | optional / recommended | Security audit |
| `process_cost_calculation` | TENANT_OWNED | required | Business data |
| `process_emission_default` | TENANT_OWNED | required | Business data |
| `process_master` | TENANT_OWNED | required | Business data |
| `process_operation` | TENANT_OWNED | required | Business data |
| `process_routing` | TENANT_OWNED | required | Business data |
| `procurement_alternative_line` | TENANT_OWNED | required | Business data |
| `procurement_alternative_set` | TENANT_OWNED | required | Business data |
| `program_volume_curve` | TENANT_OWNED | required | Business data |
| `program_volume_curve_year` | TENANT_OWNED | required | Business data |
| `purchase_order` | TENANT_OWNED | required | Business data |
| `purchase_order_line` | TENANT_OWNED | required | Business data |
| `purchase_order_tracking` | TENANT_OWNED | required | Business data |
| `purchase_po_link_event` | TENANT_OWNED | required | Business data |
| `purchase_request` | TENANT_OWNED | required | Business data |
| `purchase_request_line` | TENANT_OWNED | required | Business data |
| `questionnaire_pack` | TENANT_OWNED | required | Business data |
| `questionnaire_pack_item` | TENANT_OWNED | required | Business data |
| `rfq_attachment` | TENANT_OWNED | required | Business data |
| `rfq_clarification_request` | TENANT_OWNED | required | Business data |
| `rfq_event` | TENANT_OWNED | required | Business data |
| `rfq_line` | TENANT_OWNED | required | Business data |
| `role` | GLOBAL_REFERENCE | N/A | Shared reference |
| `sap_component_mapping` | TENANT_OWNED | batch has customer_id (V142); children via batch | Integration staging |
| `sap_cost_estimate_component_staging` | TENANT_OWNED | batch has customer_id (V142); children via batch | Integration staging |
| `sap_cost_estimate_staging` | TENANT_OWNED | batch has customer_id (V142); children via batch | Integration staging |
| `sga_margin_benchmark` | GLOBAL_REFERENCE | N/A or tenant override later | Market data |
| `should_cost_build_up` | TENANT_OWNED | required | Business data |
| `should_cost_emissions_snapshot` | TENANT_OWNED | required | Business data |
| `should_cost_quote_component` | TENANT_OWNED | required | Business data |
| `standard_cost` | TENANT_OWNED | required | Business data |
| `stored_file` | TENANT_OWNED | required | Business data |
| `supplier_after_sales_metric` | TENANT_OWNED | required | Business data |
| `supplier_capability` | TENANT_OWNED | required | Business data |
| `supplier_delivery_metric` | TENANT_OWNED | required | Business data |
| `supplier_eligibility_result` | TENANT_OWNED | required | Business data |
| `supplier_guarantee_event` | TENANT_OWNED | required | Business data |
| `supplier_guarantee_term` | TENANT_OWNED | required | Business data |
| `supplier_lifecycle_operation_block` | TENANT_OWNED | required | Business data |
| `supplier_master` | TENANT_OWNED | required | Business data |
| `supplier_performance_feedback` | TENANT_OWNED | required | Business data |
| `supplier_policy_evidence` | TENANT_OWNED | required | Business data |
| `supplier_portal_account` | TENANT_OWNED | required | Business data |
| `supplier_portal_invite` | TENANT_OWNED | required | Business data |
| `supplier_portal_rfq_access` | TENANT_OWNED | required | Business data |
| `supplier_qualification` | TENANT_OWNED | required | Business data |
| `supplier_qualification_approval_check` | TENANT_OWNED | required | Business data |
| `supplier_qualification_current` | TENANT_OWNED | required | Business data |
| `supplier_qualification_lifecycle_event` | TENANT_OWNED | required | Business data |
| `supplier_quality_metric` | TENANT_OWNED | required | Business data |
| `supplier_quote` | TENANT_OWNED | required | Business data |
| `supplier_risk` | TENANT_OWNED | required | Business data |
| `supplier_tco_calculation` | TENANT_OWNED | required | Business data |
| `supplier_warranty_claim` | TENANT_OWNED | required | Business data |
| `supplier_warranty_term` | TENANT_OWNED | required | Business data |
| `support_access_session` | TENANT_OWNED | required | Business data |
| `target_cost_record` | TENANT_OWNED | required | Business data |
| `target_cost_variance` | TENANT_OWNED | required | Business data |
| `tenant_costing_defaults` | TENANT_OWNED | required | Business data |
| `tooling_cost_calculation` | TENANT_OWNED | required | Business data |
| `tooling_record` | TENANT_OWNED | required | Business data |
| `uom_master` | GLOBAL_REFERENCE | N/A | Shared reference |
| `user_access_scope` | TENANT_OWNED | required | Business data |
| `user_invitation` | TENANT_OWNED | required | Business data |
| `user_role` | PLATFORM | via customer FK where applicable | Control plane |
| `valuation_currency_policy` | TENANT_OWNED | required | Business data |
| `vehicle_program` | TENANT_OWNED | required | Business data |
| `vendor_comparison` | TENANT_OWNED | required | Business data |
| `vendor_question` | TENANT_OWNED | required | Business data |
| `vendor_questionnaire_evaluation` | TENANT_OWNED | required | Business data |
| `vendor_questionnaire_response` | TENANT_OWNED | required | Business data |
| `vendor_recommendation` | TENANT_OWNED | required | Business data |
| `vendor_recommendation_candidate` | TENANT_OWNED | required | Business data |
| `vendor_recommendation_decision` | TENANT_OWNED | required | Business data |
| `vendor_recommendation_decision_event` | TENANT_OWNED | required | Business data |
| `vendor_recommendation_event_delivery` | TENANT_OWNED | required | Business data |
| `vendor_recommendation_explanation` | TENANT_OWNED | required | Business data |
| `vendor_scorecard_category` | TENANT_OWNED | required | Business data |
| `vendor_scorecard_result` | TENANT_OWNED | required | Business data |
| `vendor_scorecard_template` | TENANT_OWNED | required | Business data |
| `vendor_selection_notification_catalog` | TENANT_OWNED | required | Business data |
| `whatif_simulation` | TENANT_OWNED | required | Business data |
| `workflow_definition` | GLOBAL_REFERENCE | N/A | Shared workflow templates |
| `workflow_step_definition` | TENANT_OWNED | required | Business data |

## Wave coverage (migrations)
- V130–V142: progressive customer_id waves through V272
- V143 (V273): material/process cost calculation, rfq_line, rfq_attachment, make_vs_buy_simulation, costing_run

## Residual TENANT_OWNED without full service enforcement (track)
- Some SAP staging child rows inherit isolation via batch only
- approval_step_instance: isolated via parent workflow instance customer_id
- audit_log: platform-wide; filter by actor/tenant in reports recommended
