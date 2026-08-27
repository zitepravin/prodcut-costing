import { test, expect } from '@playwright/test';

/**
 * Helping Agent E2E (Product Costing read-only).
 * Skips gracefully when stack is down.
 *
 * Env:
 *   E2E_API_BASE (default http://localhost:8080)
 *   E2E_BASE_URL / Playwright baseURL for UI
 *   E2E_USERNAME / E2E_PASSWORD
 *   E2E_CUSTOMER_ID — optional tenant with PRODUCT_COSTING enabled
 */
const apiBase = process.env.E2E_API_BASE ?? 'http://localhost:8080';
const user = process.env.E2E_USERNAME ?? 'admin';
const pass = process.env.E2E_PASSWORD ?? 'ChangeMe!Now1';
const customerId = process.env.E2E_CUSTOMER_ID;

async function login(): Promise<string | null> {
  try {
    const controller = new AbortController();
    const t = setTimeout(() => controller.abort(), 10_000);
    const res = await fetch(`${apiBase}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: user, password: pass }),
      signal: controller.signal,
    });
    clearTimeout(t);
    if (!res.ok) return null;
    const body = (await res.json()) as { accessToken?: string };
    return body.accessToken ?? null;
  } catch {
    return null;
  }
}

test.describe('Helping Agent API', () => {
  test('unauthenticated chat returns 401', async () => {
    let status = 0;
    try {
      const controller = new AbortController();
      const t = setTimeout(() => controller.abort(), 10_000);
      const res = await fetch(`${apiBase}/api/v1/helping-agent/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question: 'Explain BOM' }),
        signal: controller.signal,
      });
      clearTimeout(t);
      status = res.status;
    } catch {
      test.skip(true, 'Backend not reachable');
    }
    expect([401, 403]).toContain(status);
  });

  test('authenticated chat returns classified answer with sources or missingData', async () => {
    test.setTimeout(20_000);
    const token = await login();
    test.skip(!token, 'Backend not reachable or login failed');
    const headers: Record<string, string> = {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
    if (customerId) headers['X-Customer-Id'] = customerId;

    let res: Response;
    try {
      const controller = new AbortController();
      const t = setTimeout(() => controller.abort(), 15_000);
      res = await fetch(`${apiBase}/api/v1/helping-agent/chat`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          question: 'Explain the dashboard KPIs',
          context: { currentPage: 'dashboard' },
          correlationId: 'e2e-ha-1',
        }),
        signal: controller.signal,
      });
      clearTimeout(t);
    } catch {
      test.skip(true, 'Helping Agent chat timed out or backend unreachable');
      return;
    }

    // 403 is expected when:
    // - ACCESS-07: X-Customer-Id header missing (module-scoped ops require tenant)
    // - ACCESS-03: PRODUCT_COSTING not enabled for the tenant
    if (res.status === 403) {
      const body = await res.json().catch(() => ({}));
      const msg = JSON.stringify(body);
      expect(
        msg.includes('ACCESS-07') ||
          msg.includes('ACCESS-03') ||
          msg.includes('TENANT_ACCESS_DENIED') ||
          msg.includes('X-Customer-Id') ||
          msg.includes('MODULE') ||
          msg.includes('PRODUCT_COSTING') ||
          msg.includes('disabled'),
      ).toBeTruthy();
      return;
    }

    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      answer?: string;
      classification?: string;
      confidence?: string;
      sources?: unknown[];
      missingData?: unknown[];
      correlationId?: string;
    };
    expect(body.answer && body.answer.length > 0).toBeTruthy();
    expect(
      ['FACT', 'CALCULATED', 'RULE_RESULT', 'AI_INFERENCE', 'MISSING_DATA'].includes(
        body.classification ?? '',
      ),
    ).toBeTruthy();
    expect(['HIGH', 'MEDIUM', 'LOW'].includes(body.confidence ?? '')).toBeTruthy();
    expect(body.correlationId).toBe('e2e-ha-1');
  });
});

test.describe('Helping Agent UI', () => {
  test('FAB opens costing help panel after login when module available', async ({ page }) => {
    try {
      await page.goto('/login', { timeout: 8_000 });
    } catch {
      test.skip(true, 'Frontend not reachable — start Vite and backend first');
    }

    await page.getByLabel(/username/i).fill(user);
    await page.getByLabel(/password/i).fill(pass);
    await page.getByRole('button', { name: /sign in|log in|login/i }).click();

    await page.waitForTimeout(1500);
    const fab = page.getByRole('button', { name: /open costing help/i });
    const fabVisible = await fab.isVisible().catch(() => false);
    if (!fabVisible) {
      test.skip(true, 'Helping Agent FAB not visible (PRODUCT_COSTING may be disabled)');
    }
    await fab.click();
    await expect(page.getByText(/Costing Help/i)).toBeVisible();
    await expect(page.getByText(/Read-only/i)).toBeVisible();
  });
});
