-- V58: Snapshot supplier lifecycle on recommendation candidates; BLOCKED/SUSPENDED cannot be ranked eligible
ALTER TABLE vendor_recommendation_candidate
    ADD COLUMN lifecycle_status VARCHAR(30) NULL,
    ADD COLUMN lifecycle_blocked BOOLEAN NOT NULL DEFAULT FALSE;
