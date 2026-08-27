-- V145: Process cost component breakdown (Power / Consumable / Inspection)
ALTER TABLE process_cost_calculation
    ADD COLUMN setup_cost DECIMAL(18,6) NULL AFTER total_process_cost,
    ADD COLUMN machine_cost DECIMAL(18,6) NULL AFTER setup_cost,
    ADD COLUMN direct_labor_cost DECIMAL(18,6) NULL AFTER machine_cost,
    ADD COLUMN power_cost DECIMAL(18,6) NULL AFTER direct_labor_cost,
    ADD COLUMN consumable_cost DECIMAL(18,6) NULL AFTER power_cost,
    ADD COLUMN inspection_cost DECIMAL(18,6) NULL AFTER consumable_cost,
    ADD COLUMN external_process_cost DECIMAL(18,6) NULL AFTER inspection_cost,
    ADD COLUMN component_breakdown TEXT NULL AFTER external_process_cost,
    ADD COLUMN costing_date DATE NULL AFTER component_breakdown;

-- Optional operation-level power / inspection inputs (nullable = not configured)
ALTER TABLE process_operation
    ADD COLUMN machine_kw DECIMAL(18,6) NULL AFTER output_parts_per_cycle,
    ADD COLUMN electricity_rate DECIMAL(18,6) NULL AFTER machine_kw,
    ADD COLUMN electricity_rate_currency CHAR(3) NULL AFTER electricity_rate,
    ADD COLUMN inspection_time_hours DECIMAL(18,6) NULL AFTER electricity_rate_currency,
    ADD COLUMN inspector_rate DECIMAL(18,6) NULL AFTER inspection_time_hours,
    ADD COLUMN inspection_fixed_charge DECIMAL(18,6) NULL AFTER inspector_rate;
