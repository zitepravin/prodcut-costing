# Regression Report — MASTER V140 (2026-08-14)

## Scope
Post–Helping Agent (V138/V139) full host unit regression + FE contextual prompts.

## Results
| Suite | Result | Count |
|-------|--------|-------|
| Backend unit (exclude integration.*) | **PASS** | **452** / 0 fail |
| Helping Agent unit (included above) | **PASS** | 13 |
| Flyway IT / Docker | **NOT RUN** | agent host has no DB/Docker |
| Playwright helping-agent | **NOT RUN** | stack not up; spec present |
| Frontend Vitest | **NOT RUN** this session | |

## Code changes (V140)
- Page-level Helping Agent context (`helpingAgentContext.ts`)
- BomPage / ShouldCostPage publish programId, bomNodeId, buildUpId to session
- AppLayout: path-based suggested prompts + stored context
- `scripts/helping-agent-smoke.sh` for live host verification

## Risks unchanged
- Production dry-run still ops-owned
- Docker compose still Windows/host-owned
- LLM still disabled by default
