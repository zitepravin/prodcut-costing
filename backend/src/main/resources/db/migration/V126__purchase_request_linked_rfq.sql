-- Wave C: link approved purchase request to existing RFQ (no second RFQ system)
ALTER TABLE purchase_request
    ADD COLUMN linked_rfq_id CHAR(36) NULL AFTER make_vs_buy_simulation_id;
CREATE INDEX idx_pr_linked_rfq ON purchase_request (linked_rfq_id);
