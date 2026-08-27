# QA Master Status — 2026-08-14 (V145)

| Layer | Status | Evidence |
|-------|--------|----------|
| Unit (backend, excl IT) | PASS 452 | REGRESSION_V140 |
| Flyway IT / host migrate | PASS V91 | HOST_LIVE_SMOKE / this session |
| prod-smoke | PASS | host API |
| helping-agent-smoke | PASS | host API |
| Playwright Helping Agent | PASS | V142 |
| Playwright full (ACCESS-03) | ~16 PASS | V143; 360 intermittent fixed in V144 by auth sequencing |
| Vitest | 17 PASS | V142 |
| Docker compose | NOT RUN | no Docker on agent |
| Customer prod dry-run | PENDING | ops |
