-- V50: Default Vendor Selection seed — scorecard template (weights = 100%) + starter questionnaire
-- Idempotent: only inserts when template_code / question_code version 1 not already present.
-- Generic automotive/manufacturing defaults — not company-specific. Customers may clone/version.

-- ——— Default scorecard template VS-DEFAULT-100 ———
INSERT INTO vendor_scorecard_template (
    template_id, template_code, template_name, description, status, created_at
)
SELECT UUID(), 'VS-DEFAULT-100', 'Default Supplier Scorecard (100%)',
       'Starter template: Quality 25, Cost 15, Delivery 15, Technical 10, Capacity 10, Warranty 5, Guarantee 5, Service 5, History 5, Risk 5. Weights total 100%.',
       'ACTIVE', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM vendor_scorecard_template WHERE template_code = 'VS-DEFAULT-100'
);

INSERT INTO vendor_scorecard_category (category_id, template_id, category_code, category_name, weight_percent, sequence_no, created_at)
SELECT UUID(), t.template_id, 'QUALITY', 'Quality', 25.0000, 1, UTC_TIMESTAMP(6)
FROM vendor_scorecard_template t
WHERE t.template_code = 'VS-DEFAULT-100'
  AND NOT EXISTS (SELECT 1 FROM vendor_scorecard_category c WHERE c.template_id = t.template_id AND c.category_code = 'QUALITY');

INSERT INTO vendor_scorecard_category (category_id, template_id, category_code, category_name, weight_percent, sequence_no, created_at)
SELECT UUID(), t.template_id, 'COST', 'Cost / Commercial', 15.0000, 2, UTC_TIMESTAMP(6)
FROM vendor_scorecard_template t
WHERE t.template_code = 'VS-DEFAULT-100'
  AND NOT EXISTS (SELECT 1 FROM vendor_scorecard_category c WHERE c.template_id = t.template_id AND c.category_code = 'COST');

INSERT INTO vendor_scorecard_category (category_id, template_id, category_code, category_name, weight_percent, sequence_no, created_at)
SELECT UUID(), t.template_id, 'DELIVERY', 'Delivery', 15.0000, 3, UTC_TIMESTAMP(6)
FROM vendor_scorecard_template t
WHERE t.template_code = 'VS-DEFAULT-100'
  AND NOT EXISTS (SELECT 1 FROM vendor_scorecard_category c WHERE c.template_id = t.template_id AND c.category_code = 'DELIVERY');

INSERT INTO vendor_scorecard_category (category_id, template_id, category_code, category_name, weight_percent, sequence_no, created_at)
SELECT UUID(), t.template_id, 'TECHNICAL', 'Technical Capability', 10.0000, 4, UTC_TIMESTAMP(6)
FROM vendor_scorecard_template t
WHERE t.template_code = 'VS-DEFAULT-100'
  AND NOT EXISTS (SELECT 1 FROM vendor_scorecard_category c WHERE c.template_id = t.template_id AND c.category_code = 'TECHNICAL');

INSERT INTO vendor_scorecard_category (category_id, template_id, category_code, category_name, weight_percent, sequence_no, created_at)
SELECT UUID(), t.template_id, 'CAPACITY', 'Capacity', 10.0000, 5, UTC_TIMESTAMP(6)
FROM vendor_scorecard_template t
WHERE t.template_code = 'VS-DEFAULT-100'
  AND NOT EXISTS (SELECT 1 FROM vendor_scorecard_category c WHERE c.template_id = t.template_id AND c.category_code = 'CAPACITY');

INSERT INTO vendor_scorecard_category (category_id, template_id, category_code, category_name, weight_percent, sequence_no, created_at)
SELECT UUID(), t.template_id, 'WARRANTY', 'Warranty', 5.0000, 6, UTC_TIMESTAMP(6)
FROM vendor_scorecard_template t
WHERE t.template_code = 'VS-DEFAULT-100'
  AND NOT EXISTS (SELECT 1 FROM vendor_scorecard_category c WHERE c.template_id = t.template_id AND c.category_code = 'WARRANTY');

INSERT INTO vendor_scorecard_category (category_id, template_id, category_code, category_name, weight_percent, sequence_no, created_at)
SELECT UUID(), t.template_id, 'GUARANTEE', 'Guarantee', 5.0000, 7, UTC_TIMESTAMP(6)
FROM vendor_scorecard_template t
WHERE t.template_code = 'VS-DEFAULT-100'
  AND NOT EXISTS (SELECT 1 FROM vendor_scorecard_category c WHERE c.template_id = t.template_id AND c.category_code = 'GUARANTEE');

INSERT INTO vendor_scorecard_category (category_id, template_id, category_code, category_name, weight_percent, sequence_no, created_at)
SELECT UUID(), t.template_id, 'SERVICE', 'After-Sales Service', 5.0000, 8, UTC_TIMESTAMP(6)
FROM vendor_scorecard_template t
WHERE t.template_code = 'VS-DEFAULT-100'
  AND NOT EXISTS (SELECT 1 FROM vendor_scorecard_category c WHERE c.template_id = t.template_id AND c.category_code = 'SERVICE');

INSERT INTO vendor_scorecard_category (category_id, template_id, category_code, category_name, weight_percent, sequence_no, created_at)
SELECT UUID(), t.template_id, 'HISTORY', 'Supplier History', 5.0000, 9, UTC_TIMESTAMP(6)
FROM vendor_scorecard_template t
WHERE t.template_code = 'VS-DEFAULT-100'
  AND NOT EXISTS (SELECT 1 FROM vendor_scorecard_category c WHERE c.template_id = t.template_id AND c.category_code = 'HISTORY');

INSERT INTO vendor_scorecard_category (category_id, template_id, category_code, category_name, weight_percent, sequence_no, created_at)
SELECT UUID(), t.template_id, 'RISK', 'Risk', 5.0000, 10, UTC_TIMESTAMP(6)
FROM vendor_scorecard_template t
WHERE t.template_code = 'VS-DEFAULT-100'
  AND NOT EXISTS (SELECT 1 FROM vendor_scorecard_category c WHERE c.template_id = t.template_id AND c.category_code = 'RISK');

-- ——— Starter questionnaire questions (version 1, active) ———
-- Mandatory gates: IATF (where applicable), capacity confirmation, critical quality cert

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-IATF', 1, 'COMPLIANCE',
       'Does the supplier hold a currently valid IATF 16949 certification where applicable?',
       'YES_NO', TRUE, TRUE, TRUE, 10.0000, 'YES=1 NO=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-IATF' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-ISO9001', 1, 'QUALITY',
       'Does the supplier maintain a valid ISO 9001 certification?',
       'YES_NO', TRUE, TRUE, FALSE, 5.0000, 'YES=1 NO=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-ISO9001' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-PROCESS', 1, 'TECHNICAL',
       'Can the supplier manufacture the required part with the required process?',
       'YES_NO', TRUE, FALSE, TRUE, 10.0000, 'YES=1 NO=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-PROCESS' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-CAPACITY', 1, 'CAPACITY',
       'What is the supplier maximum annual capacity for this commodity (units)?',
       'NUMBER', TRUE, FALSE, FALSE, 5.0000, 'higher_better', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-CAPACITY' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-PPM', 1, 'QUALITY',
       'What is the supplier current PPM (parts per million)?',
       'NUMBER', TRUE, TRUE, FALSE, 10.0000, 'lower_better', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-PPM' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-OTD', 1, 'DELIVERY',
       'What is the supplier on-time delivery (OTD) percentage over the last 12 months?',
       'PERCENTAGE', TRUE, TRUE, FALSE, 10.0000, 'higher_better', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-OTD' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-WARRANTY-MO', 1, 'WARRANTY',
       'What is the standard warranty duration offered (months)?',
       'NUMBER', TRUE, FALSE, FALSE, 5.0000, 'higher_better', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-WARRANTY-MO' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-BCP', 1, 'POLICY',
       'Does the supplier maintain a documented business continuity plan?',
       'YES_NO', TRUE, TRUE, FALSE, 5.0000, 'YES=1 NO=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-BCP' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-ETHICS', 1, 'COMPLIANCE',
       'Does the supplier have an anti-bribery / ethical sourcing policy with evidence?',
       'YES_NO', TRUE, TRUE, FALSE, 5.0000, 'YES=1 NO=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-ETHICS' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-SERVICE-SLA', 1, 'SERVICE',
       'What is the standard technical support response time (hours)?',
       'NUMBER', FALSE, FALSE, FALSE, 5.0000, 'lower_better', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-SERVICE-SLA' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-SINGLE-SOURCE', 1, 'RISK',
       'Is the supplier a single-source dependency for any critical commodity for this part?',
       'YES_NO', TRUE, FALSE, FALSE, 5.0000, 'NO=1 YES=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-SINGLE-SOURCE' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-TOOLING', 1, 'TECHNICAL',
       'Does the supplier have in-house tooling capability for this commodity?',
       'YES_NO', FALSE, FALSE, FALSE, 5.0000, 'YES=1 NO=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-TOOLING' AND version_no = 1);
