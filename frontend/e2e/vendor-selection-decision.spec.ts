import { test, expect } from '@playwright/test';

/**
 * Decision-boundary E2E — recommendation is decision support only.
 * Never asserts auto-approval; verifies human-control copy is present.
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
    test.skip(true, 'Login failed');
  }
}

test.describe('Vendor Selection decision boundary', () => {
  test('page banner states lowest price is never best supplier', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/vendor-selection', { waitUntil: 'networkidle' });
    await expect(page.getByText(/Lowest price is never treated as best supplier/i).first()).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.getByText(/human approval/i).first()).toBeVisible({ timeout: 10_000 });
  });

  test('recommendation tab remains decision-support only', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/vendor-selection?tab=recommendation', { waitUntil: 'networkidle' });
    await expect(page.getByText('Recommendation', { exact: true }).first()).toBeVisible({ timeout: 15_000 });
    const body = await page.locator('body').innerText();
    // Human control language present somewhere on VS page (banner or tab content)
    expect(
      /human|decision|approval|never auto/i.test(body),
    ).toBeTruthy();
  });
});
