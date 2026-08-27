-- P3-07a: system default scorecard + support for DRAFT status (ACTIVE=published, INACTIVE=retired)

ALTER TABLE vendor_scorecard_template
    ADD COLUMN is_system_default TINYINT(1) NOT NULL DEFAULT 0;

UPDATE vendor_scorecard_template
SET is_system_default = 1
WHERE template_code = 'VS-DEFAULT-100';
