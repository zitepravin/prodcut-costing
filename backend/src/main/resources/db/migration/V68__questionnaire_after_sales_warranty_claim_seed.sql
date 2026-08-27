-- V68: Expand questionnaire bank — after-sales, warranty claims, guarantee history (idempotent)
INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-AS-DEDICATED', 1, 'SERVICE',
       'Does the supplier provide dedicated after-sales support for this commodity?',
       'YES_NO', TRUE, FALSE, FALSE, 5.0000, 'YES=1 NO=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-AS-DEDICATED' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-AS-RESPONSE-HRS', 1, 'SERVICE',
       'What is the standard after-sales technical-support response time (hours)?',
       'NUMBER', TRUE, TRUE, FALSE, 5.0000, 'LOWER_IS_BETTER', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-AS-RESPONSE-HRS' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-AS-EMERGENCY', 1, 'SERVICE',
       'Is 24x7 or emergency after-sales support available?',
       'YES_NO', TRUE, FALSE, FALSE, 5.0000, 'YES=1 NO=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-AS-EMERGENCY' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-AS-SPARES', 1, 'SERVICE',
       'Are spare parts available with a defined lead time for this part family?',
       'YES_NO', TRUE, TRUE, FALSE, 5.0000, 'YES=1 NO=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-AS-SPARES' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-WC-RATE', 1, 'WARRANTY',
       'What is the warranty claim rate (claims per 1000 units) for the last 12 months?',
       'NUMBER', FALSE, TRUE, FALSE, 5.0000, 'LOWER_IS_BETTER', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-WC-RATE' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-WC-COST', 1, 'WARRANTY',
       'What was the total warranty claim cost in the last 12 months (reporting currency)?',
       'CURRENCY', FALSE, TRUE, FALSE, 5.0000, 'LOWER_IS_BETTER', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-WC-COST' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-GE-PRIOR-FAIL', 1, 'GUARANTEE',
       'Has the supplier previously failed a contractual guarantee for this commodity?',
       'YES_NO', TRUE, TRUE, FALSE, 5.0000, 'YES=0 NO=1', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-GE-PRIOR-FAIL' AND version_no = 1);

INSERT INTO vendor_question (
    question_id, question_code, version_no, category, question_text, answer_type,
    required_flag, evidence_required, mandatory_gate, weight, scoring_rule, active_flag, created_at
)
SELECT UUID(), 'VS-Q-GE-PENALTY', 1, 'GUARANTEE',
       'Are contractual penalties defined for guarantee non-achievement?',
       'YES_NO', TRUE, TRUE, FALSE, 5.0000, 'YES=1 NO=0', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_question WHERE question_code = 'VS-Q-GE-PENALTY' AND version_no = 1);
