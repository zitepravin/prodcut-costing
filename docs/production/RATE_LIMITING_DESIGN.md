# Rate Limiting Design (P0 remaining)

## Goal
Protect the SaaS platform so one customer, user, or script cannot exhaust shared CPU, DB connections, or workers.

## Current state
- Login/refresh: in-memory `LoginRateLimiter` (not multi-instance safe).
- No general API / tenant / import / report limits.

## Recommended architecture

```
Request
  → API Gateway or Spring Filter
  → Redis fixed-window or token-bucket
  → Key: rl:{customerId}:{userId}:{bucket}
  → Allow / 429 Too Many Requests
```

### Buckets (examples — configure per environment)

| Bucket | Key scope | Starting limit |
|--------|-----------|----------------|
| auth | IP + username | 20 / 5 min |
| api_standard | customer + user | 600 / min |
| upload | customer + user | 30 / min |
| report | customer | 20 / min |
| import | customer | 10 / min |
| ai | customer | 60 / min |
| admin | user | 120 / min |

## Implementation options
1. **API Gateway** (AWS API Gateway, Kong, nginx+limit_req) — preferred at edge.
2. **Spring + Bucket4j + Redis** — in-app, works with multiple backends.
3. Do **not** rely on JVM-local maps in production multi-instance.

## Failure mode
If Redis is down: fail-open with reduced global limit **or** fail-closed for non-critical buckets — product decision. Prefer fail-open for auth health with strict auth bucket still enforced at gateway.

## Noisy neighbor
Also limit concurrent background jobs per customer (report/import/AI queue depth).

## Next implementation step
Add Redis dependency + `RateLimitFilter` with Bucket4j (or gateway config) keyed by CustomerContext + principal.


## Implementation status (V261)

| Store | Property | Multi-instance |
|-------|----------|----------------|
| In-memory | `app.security.rate-limit-store=memory` (default) | No |
| Redis | `app.security.rate-limit-store=redis` | Yes |

### Enable Redis

```bash
RATE_LIMIT_STORE=redis
REDIS_HOST=redis
REDIS_PORT=6379
# REDIS_PASSWORD=...
```

Redis auto-config is excluded unless `rate-limit-store=redis`. Fail-open if Redis is unreachable (log warning). Prefer gateway limits as the hard edge control.
