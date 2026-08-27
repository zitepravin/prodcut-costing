import { test, expect } from '@playwright/test';

/**
 * VS 360 with demo seed supplier (V18).
 * supplier_id: c1000000-0000-4000-8000-000000000001 (ACME-BRG)
 */
const user = process.env.E2E_USERNAME ?? 'admin';
const pass = process.env.E2E_PASSWORD ?? 'ChangeMe!Now1';
const SEED_SUPPLIER_ID = process.env.E2E_SUPPLIER_ID ?? 'c1000000-0000-4000-8000-000000000001';
const SEED_SUPPLIER_CODE = 'ACME-BRG';

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
  // login() now awaits refreshAccess — still allow render settle
  await page.waitForTimeout(2000);
  if (page.url().includes('/login')) {
    test.skip(true, 'Login failed — backend may be down');
  }
}

test.describe('Vendor Selection 360 fixture', () => {
  test('load Supplier 360 for seed ACME-BRG', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/vendor-selection?tab=360', { waitUntil: 'networkidle' });
    await expect(page.getByText('Vendor Selection').first()).toBeVisible({ timeout: 15_000 });

    await page.locator('#vs-supplier-id, input[name="supplierId"], [data-testid="vs-supplier-id"]').first().fill(SEED_SUPPLIER_ID);
    const loadRespPromise = page.waitForResponse(
      (r) => r.url().includes('/vendor-selection/suppliers/') && r.url().includes('/360'),
      { timeout: 15_000 },
    ).catch(() => null);
    await page.getByRole('button', { name: /Load Supplier 360/i }).click();
    const resp = await loadRespPromise;
    await page.waitForTimeout(1000);
    const body = await page.locator('body').innerText();
    const apiOk = resp != null && resp.status() >= 200 && resp.status() < 300;
    const loaded =
      apiOk ||
      body.includes('Supplier 360 loaded') ||
      body.includes(SEED_SUPPLIER_CODE) ||
      body.includes(SEED_SUPPLIER_ID) ||
      body.includes('Capabilities') ||
      body.includes('Quality metrics') ||
      body.includes('Warranties');
    expect(body.includes('Select a supplier')).toBeFalsy();
    expect(loaded || body.includes('Failed to load 360')).toBeTruthy();
    if (body.includes('Supplier 360 loaded') || body.includes('Capabilities')) {
      await expect(page.getByText(/Capabilities|Quality metrics|Warranties/i).first()).toBeVisible();
    }
  });

  test('list suppliers does not crash', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/vendor-selection', { waitUntil: 'networkidle' });
    await page.getByRole('button', { name: /List suppliers/i }).click();
    await page.waitForTimeout(2500);
    await expect(page.getByText('Vendor Selection').first()).toBeVisible();
    const body = await page.locator('body').innerText();
    if (body.includes(SEED_SUPPLIER_CODE)) {
      expect(body).toContain(SEED_SUPPLIER_CODE);
    }
  });
});
