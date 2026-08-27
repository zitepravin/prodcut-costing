import { test, expect } from '@playwright/test';

/**
 * Module hierarchy breadcrumbs + Back destinations.
 * Requires live stack + logged-in session (reuse storageState if configured).
 */
const CASES: Array<{ path: string; parentCrumb: string; backLabel: string }> = [
  { path: '/make-vs-buy', parentCrumb: 'Product Costing', backLabel: 'Back to Product Costing' },
  { path: '/target-costing', parentCrumb: 'Product Costing', backLabel: 'Back to Product Costing' },
  { path: '/cost-comparison', parentCrumb: 'Product Costing', backLabel: 'Back to Product Costing' },
  { path: '/bom', parentCrumb: 'Product Costing', backLabel: 'Back to Product Costing' },
  { path: '/currency', parentCrumb: 'Master Data', backLabel: 'Back to Master Data' },
  { path: '/import-export', parentCrumb: 'Master Data', backLabel: 'Back to Master Data' },
  { path: '/vendor-selection', parentCrumb: 'Sourcing', backLabel: 'Back to Sourcing' },
  { path: '/rfq', parentCrumb: 'Sourcing', backLabel: 'Back to Sourcing' },
  { path: '/erp-staging', parentCrumb: 'Operations', backLabel: 'Back to Operations' },
  { path: '/sap-cost-estimates', parentCrumb: 'Operations', backLabel: 'Back to Operations' },
  { path: '/reports', parentCrumb: 'Operations', backLabel: 'Back to Operations' },
];

test.describe('Module hierarchy navigation', () => {
  for (const c of CASES) {
    test(`${c.path} shows ${c.parentCrumb} and ${c.backLabel}`, async ({ page }) => {
      await page.goto(c.path);
      // May redirect to login on unauthenticated CI — soft skip
      if (page.url().includes('/login')) {
        test.skip(true, 'Requires authenticated session');
      }
      await expect(page.getByLabel('breadcrumb')).toContainText(c.parentCrumb);
      await expect(page.getByRole('button', { name: c.backLabel })).toBeVisible();
      // No incorrect lone "Back to Dashboard" on module children
      await expect(page.getByRole('button', { name: 'Back to Dashboard' })).toHaveCount(0);
    });
  }
});
