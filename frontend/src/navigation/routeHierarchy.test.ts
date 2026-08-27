import { describe, expect, it } from 'vitest';
import {
  resolveRouteNavMeta,
  shouldShowBackButton,
  backButtonLabel,
} from './routeHierarchy';

describe('routeHierarchy module tree', () => {
  it('Make vs Buy → Product Costing', () => {
    const m = resolveRouteNavMeta('/make-vs-buy');
    expect(m.parentPath).toBe('/product-costing');
    expect(backButtonLabel(m)).toBe('Back to Product Costing');
    expect(m.breadcrumbs.map((b) => b.label)).toEqual([
      'Dashboard',
      'Product Costing',
      'Make vs Buy',
    ]);
    expect(m.breadcrumbs[1].to).toBe('/product-costing');
  });

  it('Currency → Master Data', () => {
    const m = resolveRouteNavMeta('/currency');
    expect(m.parentPath).toBe('/master-data');
    expect(backButtonLabel(m)).toBe('Back to Master Data');
  });

  it('Vendor Selection → Sourcing', () => {
    const m = resolveRouteNavMeta('/vendor-selection');
    expect(m.parentPath).toBe('/sourcing');
    expect(backButtonLabel(m)).toBe('Back to Sourcing');
  });

  it('ERP Staging → Operations', () => {
    const m = resolveRouteNavMeta('/erp-staging');
    expect(m.parentPath).toBe('/operations');
    expect(backButtonLabel(m)).toBe('Back to Operations');
  });

  it('Reports → Operations', () => {
    const m = resolveRouteNavMeta('/reports');
    expect(m.parentPath).toBe('/operations');
    expect(m.breadcrumbs.map((b) => b.label)).toEqual([
      'Dashboard',
      'Operations',
      'Reports',
    ]);
  });

  it('SAP Cost Estimates → Operations', () => {
    const m = resolveRouteNavMeta('/sap-cost-estimates');
    expect(m.parentPath).toBe('/operations');
    expect(backButtonLabel(m)).toBe('Back to Operations');
  });

  it('module landing backs to Dashboard', () => {
    const m = resolveRouteNavMeta('/product-costing');
    expect(m.isModuleLanding).toBe(true);
    expect(m.parentPath).toBe('/');
    expect(shouldShowBackButton(m)).toBe(true);
    expect(backButtonLabel(m)).toBe('Back to Dashboard');
  });

  it('dashboard has no back', () => {
    const m = resolveRouteNavMeta('/');
    expect(shouldShowBackButton(m)).toBe(false);
  });
});
