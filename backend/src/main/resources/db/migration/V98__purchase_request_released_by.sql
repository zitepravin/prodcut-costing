-- P3-01 hardening D: audit actor on release
ALTER TABLE purchase_request
    ADD COLUMN released_by VARCHAR(36) NULL AFTER released_at;
