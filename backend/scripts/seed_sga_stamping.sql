-- Optional smoke seed: SG&A/margin benchmarks for Should-Cost generate
INSERT INTO sga_margin_benchmark (benchmark_id, commodity_family, region, sga_pct, margin_pct, active, created_at)
SELECT UUID(), 'STAMPING', 'NA', 0.100000, 0.080000, TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
  SELECT 1 FROM sga_margin_benchmark WHERE commodity_family='STAMPING' AND region='NA' AND active=TRUE
);
INSERT INTO sga_margin_benchmark (benchmark_id, commodity_family, region, sga_pct, margin_pct, active, created_at)
SELECT UUID(), 'STAMPING', 'NA + India', 0.100000, 0.080000, TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
  SELECT 1 FROM sga_margin_benchmark WHERE commodity_family='STAMPING' AND region='NA + India' AND active=TRUE
);
