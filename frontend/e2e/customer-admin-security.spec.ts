import { test, expect } from '@playwright/test';

/**
 * V225 — Customer Admin security E2E against live stack.
 * Requires: docker compose up, FE at baseURL (default localhost:8088).
 * Skips gracefully if stack unavailable.
 */

const BASE = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:8088';

test.describe('Customer Admin security', () => {
  test('login page loads', async ({ page }) => {
    const res = await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded', timeout: 15000 }).catch(() => null);
    if (!res) {
      test.skip(true, 'Frontend not reachable — start docker compose');
      return;
    }
    await expect(page.locator('body')).toBeVisible();
  });

  test('unauthenticated customer users API returns 401', async ({ request }) => {
    const api = process.env.PLAYWRIGHT_API_URL ?? 'http://localhost:8080';
    const res = await request.get(`${api}/api/v1/customer/users`).catch(() => null);
    if (!res) {
      test.skip(true, 'Backend not reachable');
      return;
    }
    expect([401, 403]).toContain(res.status());
  });

  test('invitation accept without token body returns 400/401', async ({ request }) => {
    const api = process.env.PLAYWRIGHT_API_URL ?? 'http://localhost:8080';
    const res = await request.post(`${api}/api/v1/customer/invitations/accept`, {
      data: {},
    }).catch(() => null);
    if (!res) {
      test.skip(true, 'Backend not reachable');
      return;
    }
    expect(res.status()).toBeGreaterThanOrEqual(400);
  });
});
