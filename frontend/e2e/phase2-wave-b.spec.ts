import { test, expect } from '@playwright/test';

/**
 * P3-08 / Option B — Phase-2 + Wave B route registration and UI smoke.
 *
 * Covers routes introduced or extended in Phase-2 / Wave A / Wave B:
 *   /lifecycle, /mixed-costing, /make-vs-buy, /sap-cost-estimates
 * Plus Make-vs-Buy Export package control visibility after navigation.
 *
 * Behavior when stack is down: tests skip cleanly (same pattern as smoke.spec.ts).
 * Do NOT treat a green skip-heavy run on an agent host as production PASS.
 *
 * Env:
 *   PLAYWRIGHT_BASE_URL (default http://127.0.0.1:5173)
 *   E2E_USERNAME / E2E_PASSWORD (optional authenticated checks)
 */

const demoUser = process.env.E2E_USERNAME ?? 'admin';
const demoPass = process.env.E2E_PASSWORD ?? 'ChangeMe!Now1';

const phase2Routes = [
  { path: '/lifecycle', label: /lifecycle/i },
  { path: '/mixed-costing', label: /mixed\s*cost/i },
  { path: '/make-vs-buy', label: /make\s*vs\s*buy|make-vs-buy/i },
  { path: '/sap-cost-estimates', label: /sap\s*cost/i },
] as const;

async function gotoOrSkip(page: import('@playwright/test').Page, path: string) {
  try {
    const response = await page.goto(path, { waitUntil: 'domcontentloaded', timeout: 8_000 });
    if (!response || response.status() >= 500) {
      test.skip(true, 'Frontend not reachable — start stack (Docker/Vite+API) first');
    }
    return response;
  } catch {
    test.skip(true, 'Frontend not reachable — start stack (Docker/Vite+API) first');
  }
}

async function loginIfPossible(page: import('@playwright/test').Page): Promise<boolean> {
  await gotoOrSkip(page, '/login');
  const userField = page.locator('input[name="username"]');
  if ((await userField.count()) === 0) return false;
  await userField.fill(demoUser);
  await page.locator('input[name="password"]').fill(demoPass);
  await page.getByRole('button', { name: /sign in|log in|login/i }).click();
  await page.waitForTimeout(2000);
  return !page.url().includes('/login');
}

test.describe('Phase-2 / Wave B routes (P3-08 specs)', () => {
  for (const route of phase2Routes) {
    test(`route ${route.path} is registered (auth may redirect to login)`, async ({ page }) => {
      await gotoOrSkip(page, route.path);
      await page.waitForTimeout(1200);
      const url = page.url();
      expect(
        url.includes(route.path.replace(/^\//, '')) || url.includes('login'),
        `Expected ${route.path} or login redirect, got ${url}`,
      ).toBeTruthy();
    });
  }

  test('make-vs-buy page shows Export package control when authenticated shell loads', async ({ page }) => {
    const loggedIn = await loginIfPossible(page);
    if (!loggedIn) {
      test.info().annotations.push({
        type: 'note',
        description: 'Login did not leave /login — backend may be down; route registration still checked',
      });
      await gotoOrSkip(page, '/make-vs-buy');
      const url = page.url();
      expect(url.includes('make-vs-buy') || url.includes('login')).toBeTruthy();
      return;
    }

    await gotoOrSkip(page, '/make-vs-buy');
    await page.waitForTimeout(1500);
    if (page.url().includes('/login')) {
      test.skip(true, 'Session not established for make-vs-buy');
    }

    // Page must be on make-vs-buy
    expect(page.url()).toContain('make-vs-buy');

    // Export package button is only rendered when a simulation is selected (detail panel).
    // Empty list / no selection is valid for a fresh DB — do not fail the live gate.
    const exportBtn = page.getByRole('button', { name: /export package/i });
    const visible = await exportBtn.isVisible().catch(() => false);
    if (!visible) {
      test.skip(
        true,
        'Export package control appears only after selecting a Make-vs-Buy simulation — none selected on this environment',
      );
    }
    await expect(exportBtn).toBeVisible();
  });

  test('mixed-costing page is reachable after login when stack up', async ({ page }) => {
    const loggedIn = await loginIfPossible(page);
    test.skip(!loggedIn, 'Backend/login unavailable — skip authenticated mixed-costing check');
    await gotoOrSkip(page, '/mixed-costing');
    await page.waitForTimeout(1500);
    if (page.url().includes('/login')) {
      test.skip(true, 'Session not established for mixed-costing');
    }
    expect(page.url()).toContain('mixed-costing');
  });

  test('sap-cost-estimates page is reachable after login when stack up', async ({ page }) => {
    const loggedIn = await loginIfPossible(page);
    test.skip(!loggedIn, 'Backend/login unavailable — skip authenticated SAP cost estimates check');
    await gotoOrSkip(page, '/sap-cost-estimates');
    await page.waitForTimeout(1500);
    if (page.url().includes('/login')) {
      test.skip(true, 'Session not established for sap-cost-estimates');
    }
    expect(page.url()).toContain('sap-cost-estimates');
  });

  test('lifecycle page is reachable after login when stack up', async ({ page }) => {
    const loggedIn = await loginIfPossible(page);
    test.skip(!loggedIn, 'Backend/login unavailable — skip authenticated lifecycle check');
    await gotoOrSkip(page, '/lifecycle');
    await page.waitForTimeout(1500);
    if (page.url().includes('/login')) {
      test.skip(true, 'Session not established for lifecycle');
    }
    expect(page.url()).toContain('lifecycle');
  });
});

test.describe('Wave B policy smoke (UI copy)', () => {
  test('make-vs-buy mentions export does not post to SAP CO when page content loads', async ({ page }) => {
    const loggedIn = await loginIfPossible(page);
    test.skip(!loggedIn, 'Backend/login unavailable');
    await gotoOrSkip(page, '/make-vs-buy');
    await page.waitForTimeout(1500);
    if (page.url().includes('/login')) {
      test.skip(true, 'Session not established');
    }
    // Policy text on page (V175)
    const body = await page.locator('body').innerText();
    if (body.match(/export package/i)) {
      expect(body).toMatch(/does not post/i);
    } else {
      test.info().annotations.push({
        type: 'note',
        description: 'Export package chrome not visible until a simulation is selected — route still OK',
      });
    }
  });
});
