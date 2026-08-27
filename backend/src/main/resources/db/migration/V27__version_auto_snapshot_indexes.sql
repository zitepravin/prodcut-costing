-- V27: Support M15 automatic version snapshots (VER-01) — query performance for timelines
-- and optional linkage from domain events.
CREATE INDEX idx_cost_version_trigger_time
    ON cost_version_record (change_trigger, created_at);

CREATE INDEX idx_cost_version_entity_baseline
    ON cost_version_record (entity_type, entity_id, program_baseline, version_number);
