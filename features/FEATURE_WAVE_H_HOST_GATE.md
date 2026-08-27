# Wave H — Host Docker + Playwright gate

## Agent environment
| Check | Result |
|-------|--------|
| Docker CLI | **NOT AVAILABLE** |
| Docker daemon | **NOT AVAILABLE** |
| Live compose build | **BLOCKED** |
| Live Flyway V128 | **BLOCKED** |
| Live PO HTTP smoke | **BLOCKED** |
| Playwright browser | **BLOCKED** |

Offline Waves A–G remain **DONE** (unit/security tests).

## Host procedure (authoritative)

```bash
cd PRODUCT-COSTING-SOFTWARE
export JWT_SECRET="$(openssl rand -base64 48)"
# Optional: export CUSTOMER_ID=<PURCHASE-entitled uuid>
# Optional: export RUN_PLAYWRIGHT=1

./scripts/wave-h-host-gate.sh
```

Or stepwise:
1. `docker compose up --build -d`
2. Confirm health `http://127.0.0.1:8080/actuator/health`
3. Confirm Flyway applied through **V128** in backend logs
4. `CUSTOMER_ID=... ./scripts/purchase-order-smoke.sh`
5. Optional Playwright from `frontend/`

## Scripts
- `scripts/wave-h-host-gate.sh` — full host gate
- `scripts/purchase-order-smoke.sh` — create→submit→decide→issue
- `scripts/v238-host-po-gate.sh` — earlier PO gate helper

## Definition of done (host evidence required)
- [ ] Docker images build without error
- [ ] Backend health UP
- [ ] Flyway V127 PO + V128 email tables applied
- [ ] PO smoke PASS on PURCHASE tenant
- [ ] Optional: Playwright matrix PASS
