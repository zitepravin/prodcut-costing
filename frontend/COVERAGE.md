# Coverage policy (95% gate)

## Backend
- Tool: JaCoCo 0.8.12
- Gate: `mvn verify` checks **CLASS** coverage for `*TransitionEngine` ≥ **95%** line, branch, method
- Scope of mandatory gate (modified production logic): transition engines
- Full project bundle coverage is measured via `target/site/jacoco` but not yet at 95% globally (legacy modules)

## Frontend
- Tool: Vitest + `@vitest/coverage-v8`
- Gate: `npm run test:coverage` thresholds ≥ **95%** for `src/utils/**`
- Expand `coverage.include` as pure modules are extracted from pages

## Rule
WRITE CODE → WRITE TESTS → RUN TESTS → CHECK COVERAGE → FIND GAPS → ADD TESTS → VERIFY ≥95% before declaring a version complete for gated packages.
