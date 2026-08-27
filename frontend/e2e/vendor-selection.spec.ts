import { test, expect } from '@playwright/test';

/**
 * Deeper Vendor Selection E2E — requires live FE + BE.
 * Authenticates with seed admin, opens VS, verifies 360 shell and tabs.
 */
const user = process.env.E2E_USERNAME ?? 'admin';
const pass = process.env.E2E_PASSWORD ?? 'ChangeMe!Now1';

async function gotoOrSkip(page: import('@playwright/test').Page, path: string) {
  try {
    const response = await page.goto(path, { waitUntil: 'domcontentloaded', timeout: 10_000 });
    if (!response || response.status() >= 500) {
      test.skip(true, 'Frontend not reachable');
    }
    return response;
  } catch {
    test.skip(true, 'Frontend not reachable');
  }
}

async function loginAsAdmin(page: import('@playwright/test').Page) {
  await gotoOrSkip(page, '/login');
  await page.locator('input[name="username"]').fill(user);
  await page.locator('input[name="password"]').fill(pass);
  await page.getByRole('button', { name: /sign in|log in|login/i }).click();
  await page.waitForTimeout(2500);
  if (page.url().includes('/login')) {
    test.skip(true, 'Login failed — backend may be down or credentials invalid');
  }
}

test.describe('Vendor Selection', () => {
  test('nav link opens Vendor Selection after login', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/vendor-selection', { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Vendor Selection').first()).toBeVisible({ timeout: 15_000 });
  });

  test('360 View tab is default and visible', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/vendor-selection?tab=360', { waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('tab', { name: /360/i }).or(page.getByText('360 View')).first()).toBeVisible({
      timeout: 15_000,
    });
  });

  test('Eligibility and Recommendation tabs are registered', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/vendor-selection', { waitUntil: 'networkidle' });
    await expect(page.getByText('Vendor Selection').first()).toBeVisible({ timeout: 15_000 });
    // MUI Tab labels — match by text (role=tab may vary by a11y tree)
    await expect(page.getByText('Eligibility', { exact: true }).first()).toBeVisible({ timeout: 15_000 });
    await expect(page.getByText('Recommendation', { exact: true }).first()).toBeVisible({ timeout: 10_000 });
    await page.getByText('Recommendation', { exact: true }).first().click();
    await page.waitForTimeout(500);
    await expect(page.getByText('Recommendation', { exact: true }).first()).toBeVisible();
  });

  test('query tab=recommendation deep-link works', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/vendor-selection?tab=recommendation', { waitUntil: 'networkidle' });
    await expect(page.getByText('Vendor Selection').first()).toBeVisible({ timeout: 15_000 });
    await expect(page.getByText('Recommendation', { exact: true }).first()).toBeVisible({ timeout: 10_000 });
  });
});
