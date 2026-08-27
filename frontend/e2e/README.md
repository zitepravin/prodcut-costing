# Playwright smoke E2E

## Prerequisites
1. Backend running (MySQL + Spring Boot) on configured API URL
2. Frontend: `npm run dev` (default http://127.0.0.1:5173)

## Run
```bash
npx playwright install chromium
PLAYWRIGHT_BASE_URL=http://127.0.0.1:5173 npm run test:e2e
```

Optional: `E2E_USERNAME` / `E2E_PASSWORD` (defaults: demo.engineer / ChangeMe!Now1).

Tests skip gracefully when the frontend is not reachable.
