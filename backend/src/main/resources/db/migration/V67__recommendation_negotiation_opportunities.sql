-- V67: Negotiation opportunity notes on recommendation explanation
ALTER TABLE vendor_recommendation_explanation
    ADD COLUMN negotiation_opportunities VARCHAR(2000) NULL AFTER cost_variance_notes;
