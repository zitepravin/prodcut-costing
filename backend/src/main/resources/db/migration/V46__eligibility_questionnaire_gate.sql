-- V46: Link eligibility evaluation to questionnaire evaluation snapshot (optional mandatory gate)
ALTER TABLE supplier_eligibility_result
    ADD COLUMN questionnaire_evaluation_id VARCHAR(36) NULL,
    ADD COLUMN require_questionnaire BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN questionnaire_eligible BOOLEAN NULL;

CREATE INDEX idx_ser_questionnaire_eval
    ON supplier_eligibility_result (questionnaire_evaluation_id);
