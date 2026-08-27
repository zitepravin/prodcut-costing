-- V22: Denormalize program_id onto should_cost_build_up for program-scoped report aggregates.
-- Avoids correlated IN (SELECT bom_node_id FROM bom_node WHERE program_id = ?) on every report.
-- Backfill from bom_node; new rows set program_id in ShouldCostService on create.

ALTER TABLE should_cost_build_up
    ADD COLUMN program_id CHAR(36) NULL AFTER bom_node_id;

UPDATE should_cost_build_up sc
    INNER JOIN bom_node bn ON bn.bom_node_id = sc.bom_node_id
SET sc.program_id = bn.program_id
WHERE sc.program_id IS NULL;

CREATE INDEX idx_should_cost_build_up_program
    ON should_cost_build_up (program_id, calculated_at);

-- Optional FK: keep soft to avoid blocking historical rows if BOM node deleted (BOM uses restrict typically)
-- ALTER not adding FK to reduce migration risk on demo data; program_id is denormalized analytics column.
