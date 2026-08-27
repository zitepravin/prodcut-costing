-- V21: Drop redundant bom_node (program_id, status) index added in V20.
-- V3 already provides idx_bom_node_program (program_id).

DROP INDEX idx_bom_node_program_status ON bom_node;
