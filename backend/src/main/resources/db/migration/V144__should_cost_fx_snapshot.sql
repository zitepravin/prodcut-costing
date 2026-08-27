ALTER TABLE should_cost_build_up
    ADD COLUMN fx_source_currency CHAR(3) NULL AFTER calculation_basis,
    ADD COLUMN fx_target_currency CHAR(3) NULL AFTER fx_source_currency,
    ADD COLUMN fx_applied_rate DECIMAL(18,8) NULL AFTER fx_target_currency,
    ADD COLUMN fx_rate_effective_date DATE NULL AFTER fx_applied_rate,
    ADD COLUMN fx_as_of_date DATE NULL AFTER fx_rate_effective_date,
    ADD COLUMN fx_conversion_method VARCHAR(20) NULL AFTER fx_as_of_date,
    ADD COLUMN fx_rate_source VARCHAR(100) NULL AFTER fx_conversion_method;
