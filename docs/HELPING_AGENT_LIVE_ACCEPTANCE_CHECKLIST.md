# Helping Agent — Live Acceptance Checklist (Final Customer Spec)

**Environment:** Customer Windows stack (Frontend :8088, Backend :8080, MySQL :3307)  
**Package:** MASTER V151+  
**Rule:** Mark PASS only with observed evidence. Do not invent PASS.

## 0. Pre-flight

| # | Check | Result |
|---|--------|--------|
| 0.1 | `curl http://localhost:8080/actuator/health` → UP | |
| 0.2 | Frontend loads `http://localhost:8088` | |
| 0.3 | Login as entitled user (PRODUCT_COSTING enabled) | |
| 0.4 | `X-Customer-Id` set (DEFAULT tenant preferred) | |

```bash
BASE=http://127.0.0.1:8080 bash scripts/helping-agent-smoke.sh
```

## 1. Passive icon (no intervention)

| # | Check | Result |
|---|--------|--------|
| 1.1 | After login, agent FAB visible bottom-right | |
| 1.2 | Help drawer **does not** open automatically | |
| 1.3 | No chat API calls until user opens and asks | |
| 1.4 | Tooltip indicates optional / does not open by itself | |

## 2. Open and ask

| # | Check | Result |
|---|--------|--------|
| 2.1 | Click FAB → drawer “Costing Help” | |
| 2.2 | Static intro only until user asks | |
| 2.3 | Suggestion chips visible | |
| 2.4 | Type question + Send returns answer | |

## 3. Functional questions (with program/BOM context preferred)

| # | Question | Expect | Result |
|---|----------|--------|--------|
| 3.1 | Explain this BOM | FACT or data / MISSING_DATA if no context | |
| 3.2 | What is driving the cost? | Drivers or MISSING_DATA | |
| 3.3 | How is the should-cost built? | Should-cost tool data | |
| 3.4 | Why are we above/below target? | Target/variance or MISSING_DATA | |
| 3.5 | What changed from previous revision? | Versions or MISSING_DATA | |
| 3.6 | How can I investigate cost reduction? | **AI_RECOMMENDATION** labeled | |
| 3.7 | What FX rates are available? | Currency context | |
| 3.8 | What is missing? | MISSING_DATA items listed | |

## 4. Classification & honesty

| # | Check | Result |
|---|--------|--------|
| 4.1 | Response shows classification chip | |
| 4.2 | Confidence shown | |
| 4.3 | Sources shown when data returned | |
| 4.4 | No invented material/supplier prices | |
| 4.5 | AI recommendation never auto-changes data | |

## 5. Security

| # | Check | Result |
|---|--------|--------|
| 5.1 | Anonymous `POST /api/v1/helping-agent/chat` → 401/403 | |
| 5.2 | Auth without `X-Customer-Id` → 403 ACCESS-07 | |
| 5.3 | Tenant without PRODUCT_COSTING → no FAB / API denied | |
| 5.4 | Cannot approve supplier or write BOM via chat | |

## 6. Regression (existing app)

| # | Check | Result |
|---|--------|--------|
| 6.1 | BOM / Should-Cost pages still work | |
| 6.2 | `bash scripts/prod-smoke.sh` PASS | |
| 6.3 | Playwright `helping-agent.spec.ts` PASS | |

## Sign-off

| Role | Name | Date | Status |
|------|------|------|--------|
| Tester | | | |
| Product owner | | | PASS / FAIL |

