-- Wave B: Purchase Requisition commercial fields (evolve purchase_request; no second RFQ entity)
-- Existing P3-01 MvB flow remains valid; new columns nullable for backward compatibility.

ALTER TABLE purchase_request
    ADD COLUMN pr_number           VARCHAR(40)   NULL AFTER purchase_request_id,
    ADD COLUMN plant_code          VARCHAR(40)   NULL AFTER customer_id,
    ADD COLUMN requester_user_id   VARCHAR(36)   NULL AFTER plant_code,
    ADD COLUMN department_code     VARCHAR(80)   NULL AFTER requester_user_id,
    ADD COLUMN buyer_user_id       VARCHAR(36)   NULL AFTER department_code,
    ADD COLUMN required_date       DATE          NULL AFTER buyer_user_id,
    ADD COLUMN priority_code       VARCHAR(20)   NULL AFTER required_date,
    ADD COLUMN purpose_text        VARCHAR(500)  NULL AFTER priority_code;

-- Optional supplier on line: PR may exist before RFQ award (Wave C)
ALTER TABLE purchase_request_line
    MODIFY COLUMN supplier_id CHAR(36) NULL,
    ADD COLUMN material_code       VARCHAR(80)   NULL AFTER line_no,
    ADD COLUMN material_description VARCHAR(255) NULL AFTER material_code,
    ADD COLUMN target_price        DECIMAL(18,6) NULL AFTER uom,
    ADD COLUMN target_currency     CHAR(3)       NULL AFTER target_price,
    ADD COLUMN cost_center_code    VARCHAR(40)   NULL AFTER target_currency;

CREATE UNIQUE INDEX uk_pr_number_customer ON purchase_request (customer_id, pr_number);

-- Status vocabulary (application-enforced):
-- DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, CANCELLED, RELEASED
