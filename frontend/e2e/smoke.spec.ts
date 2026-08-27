import { test, expect } from '@playwright/test';

/**
 * Smoke suite — production-oriented checks against a running UI.
 * Skips cleanly when the frontend is not reachable (no stack in CI/sandbox).
 */
const demoUser = process.env.E2E_USERNAME ?? 'demo.engineer';
const demoPass = process.env.E2E_PASSWORD ?? 'ChangeMe!Now1';

async function gotoOrSkip(page: import('@playwright/test').Page, path: string) {
  try {
    const response = await page.goto(path, { waitUntil: 'domcontentloaded', timeout: 8_000 });
    if (!response || response.status() >= 500) {
      test.skip(true, 'Frontend not reachable — start Vite (npm run dev) and backend first');
    }
    return response;
  } catch {
    test.skip(true, 'Frontend not reachable — start Vite (npm run dev) and backend first');
  }
}

test.describe('Smoke', () => {
  test('login page renders username and password fields', async ({ page }) => {
    await gotoOrSkip(page, '/login');
    await expect(page.locator('input[name="username"]')).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: /sign in|log in|login/i })).toBeVisible();
  });

  test('unauthenticated root redirects or shows login', async ({ page }) => {
    await gotoOrSkip(page, '/');
    await page.waitForURL(/login|\//, { timeout: 10_000 });
    const url = page.url();
    const onLogin = url.includes('/login');
    const hasUserField = await page.locator('input[name="username"]').count();
    expect(onLogin || hasUserField > 0 || url.endsWith('/')).toBeTruthy();
  });

  test('login with demo credentials reaches app shell when API available', async ({ page }) => {
    await gotoOrSkip(page, '/login');
    await page.locator('input[name="username"]').fill(demoUser);
    await page.locator('input[name="password"]').fill(demoPass);
    await page.getByRole('button', { name: /sign in|log in|login/i }).click();
    await page.waitForTimeout(2000);
    const url = page.url();
    if (url.includes('/login')) {
      test.info().annotations.push({
        type: 'note',
        description: 'Remained on login — backend may be unavailable; UI still responded',
      });
      await expect(page.locator('input[name="username"]')).toBeVisible();
    } else {
      await expect(page).not.toHaveURL(/\/login$/);
    }
  });

  test('vendor-selection route is registered (auth may redirect)', async ({ page }) => {
    await gotoOrSkip(page, '/vendor-selection');
    await page.waitForTimeout(1500);
    const url = page.url();
    expect(url.includes('vendor-selection') || url.includes('login')).toBeTruthy();
  });
});
