-- M14 Quotation & Costing Approval Workflow (SRS 4.14, TDD 5.14 / 4.6, Test Plan 7.14). WF-01..WF-07.

CREATE TABLE workflow_definition (
    workflow_definition_id  CHAR(36)     NOT NULL PRIMARY KEY,
    name                       VARCHAR(200) NOT NULL,
    entity_type                  VARCHAR(50)  NOT NULL,
    active                          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by                        VARCHAR(36),
    created_at                          DATETIME(6)  NOT NULL,
    updated_by                            VARCHAR(36),
    updated_at                              DATETIME(6)
) ENGINE = InnoDB;

-- idempotent: idx_workflow_definition_entity_type
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'workflow_definition' AND index_name = 'idx_workflow_definition_entity_type'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_workflow_definition_entity_type ON workflow_definition (entity_type, active)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- WF-02: routing rule columns are all nullable ("no constraint on this dimension"),
-- matching UserAccessScope's (M18) absence-means-unrestricted convention.
CREATE TABLE workflow_step_definition (
    step_definition_id  CHAR(36)      NOT NULL PRIMARY KEY,
    workflow_definition_id CHAR(36)      NOT NULL,
    sequence_number           INT           NOT NULL,
    approver_role               VARCHAR(50)   NOT NULL,
    min_value                     NUMERIC(18,6),
    max_value                       NUMERIC(18,6),
    commodity_family                  VARCHAR(100),
    plant_code                          VARCHAR(50),
    escalation_hours                      INT,
    created_by                              VARCHAR(36),
    created_at                                DATETIME(6)   NOT NULL,
    updated_by                                  VARCHAR(36),
    updated_at                                    DATETIME(6),
    CONSTRAINT fk_workflow_step_def_definition FOREIGN KEY (workflow_definition_id)
        REFERENCES workflow_definition (workflow_definition_id)
) ENGINE = InnoDB;

-- idempotent: idx_workflow_step_def_definition
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'workflow_step_definition' AND index_name = 'idx_workflow_step_def_definition'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_workflow_step_def_definition ON workflow_step_definition (workflow_definition_id, sequence_number)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- entity_type/entity_id is a generic reference, not a FK - see WorkflowDefinition's
-- Javadoc for why (no single unifying cost_record table exists yet).
CREATE TABLE approval_workflow_instance (
    workflow_instance_id  CHAR(36)     NOT NULL PRIMARY KEY,
    workflow_definition_id  CHAR(36)     NOT NULL,
    entity_type                VARCHAR(50)  NOT NULL,
    entity_id                    CHAR(36)     NOT NULL,
    status                          VARCHAR(20)  NOT NULL,
    submitted_by                      CHAR(36)     NOT NULL,
    submitted_at                        DATETIME(6)  NOT NULL,
    completed_at                          DATETIME(6),
    created_by                              VARCHAR(36),
    created_at                                DATETIME(6)  NOT NULL,
    updated_by                                  VARCHAR(36),
    updated_at                                    DATETIME(6),
    CONSTRAINT fk_workflow_instance_definition FOREIGN KEY (workflow_definition_id)
        REFERENCES workflow_definition (workflow_definition_id),
    CONSTRAINT fk_workflow_instance_submitter FOREIGN KEY (submitted_by) REFERENCES app_user (user_id)
) ENGINE = InnoDB;

-- idempotent: idx_workflow_instance_entity
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'approval_workflow_instance' AND index_name = 'idx_workflow_instance_entity'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_workflow_instance_entity ON approval_workflow_instance (entity_type, entity_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- WF-07: immutable once decision moves off PENDING - enforced at the service layer
-- (ApprovalWorkflowService never updates a step whose decision != PENDING), same pattern
-- as VER-01's application-layer-plus-documented-convention enforcement elsewhere in this
-- system (a DB-level check constraint blocking UPDATE when decision <> 'PENDING' would be
-- a stronger second line of defense, matching TDD Section 4.8's "second line of defense"
-- language for cost_record - not added this session, flagged as a hardening follow-up).
CREATE TABLE approval_step_instance (
    step_instance_id      CHAR(36)     NOT NULL PRIMARY KEY,
    workflow_instance_id    CHAR(36)     NOT NULL,
    sequence_number            INT          NOT NULL,
    approver_role                VARCHAR(50)  NOT NULL,
    approver_user_id               CHAR(36),
    decision                         VARCHAR(20)  NOT NULL,
    decided_at                         DATETIME(6),
    comment                              VARCHAR(1000),
    due_at                                 DATETIME(6),
    escalated                                BOOLEAN      NOT NULL DEFAULT FALSE,
    override_flag                              BOOLEAN      NOT NULL DEFAULT FALSE,
    override_justification                       VARCHAR(1000),
    created_by                                     VARCHAR(36),
    created_at                                       DATETIME(6)  NOT NULL,
    updated_by                                         VARCHAR(36),
    updated_at                                           DATETIME(6),
    CONSTRAINT fk_approval_step_instance_workflow FOREIGN KEY (workflow_instance_id)
        REFERENCES approval_workflow_instance (workflow_instance_id),
    CONSTRAINT fk_approval_step_instance_approver FOREIGN KEY (approver_user_id) REFERENCES app_user (user_id)
) ENGINE = InnoDB;

-- idempotent: idx_approval_step_instance_workflow
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'approval_step_instance' AND index_name = 'idx_approval_step_instance_workflow'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_approval_step_instance_workflow ON approval_step_instance (workflow_instance_id, sequence_number)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_approval_step_instance_role_decision
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'approval_step_instance' AND index_name = 'idx_approval_step_instance_role_decision'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_approval_step_instance_role_decision ON approval_step_instance (approver_role, decision)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_approval_step_instance_escalation
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'approval_step_instance' AND index_name = 'idx_approval_step_instance_escalation'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_approval_step_instance_escalation ON approval_step_instance (decision, escalated, due_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- SRS 4.14.5: the "Approval Override" permission is a dedicated, explicitly-granted role
-- (see ApprovalWorkflowService's Javadoc), not folded into an existing job-title role.
-- SRS 4.14.7: M14's roles also include Purchasing Manager, not among the 10 roles V1
-- seeded (Purchasing/Sourcing Buyer -> PURCHASING_BUYER already exists, but "Manager" is
-- a distinct role from "Buyer" per the SRS's own role list).
INSERT INTO role (role_id, role_code, description, is_system_role) VALUES
    (UUID(), 'APPROVAL_OVERRIDE', 'Explicit permission to bypass/skip an approval step with logged justification (SRS 4.14.5)', TRUE),
    (UUID(), 'PURCHASING_MANAGER', 'Approves purchasing-related cost records per delegation of authority', TRUE);
