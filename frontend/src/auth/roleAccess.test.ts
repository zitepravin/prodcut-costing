import { describe, expect, it } from 'vitest';
import { canAccessPath, moduleRequiredForPath } from './roleAccess';

const none = {
  hasRole: () => false,
  hasModule: () => false,
};

const purchaseOnly = {
  hasRole: () => false,
  hasModule: (m: string) => m === 'PURCHASE',
};

const costingOnly = {
  hasRole: () => false,
  hasModule: (m: string) => m === 'PRODUCT_COSTING',
};

const vsOnly = {
  hasRole: () => false,
  hasModule: (m: string) => m === 'VENDOR_SELECTION',
};

describe('Wave A module path access', () => {
  it('purchase-only can open purchase-requests but not bom or vendor-selection', () => {
    expect(canAccessPath('/purchase-requests', purchaseOnly)).toBe(true);
    expect(canAccessPath('/bom', purchaseOnly)).toBe(false);
    expect(canAccessPath('/vendor-selection', purchaseOnly)).toBe(false);
    expect(canAccessPath('/rfq', purchaseOnly)).toBe(false);
  });

  it('costing-only can open bom/rfq/universal-costing but not purchase-requests', () => {
    expect(canAccessPath('/bom', costingOnly)).toBe(true);
    expect(canAccessPath('/rfq', costingOnly)).toBe(true);
    expect(canAccessPath('/universal-costing', costingOnly)).toBe(true);
    expect(canAccessPath('/universal-costing', purchaseOnly)).toBe(false);
    expect(canAccessPath('/purchase-requests', costingOnly)).toBe(false);
    expect(canAccessPath('/vendor-selection', costingOnly)).toBe(false);
  });

  it('vs-only can open vendor-selection only among module paths', () => {
    expect(canAccessPath('/vendor-selection', vsOnly)).toBe(true);
    expect(canAccessPath('/purchase-requests', vsOnly)).toBe(false);
    expect(canAccessPath('/should-cost', vsOnly)).toBe(false);
  });

  it('no modules → no module paths', () => {
    expect(canAccessPath('/purchase-requests', none)).toBe(false);
    expect(canAccessPath('/materials', none)).toBe(false);
  });

  it('moduleRequiredForPath is stable', () => {
    expect(moduleRequiredForPath('/purchase-requests')).toBe('PURCHASE');
    expect(moduleRequiredForPath('/vendor-selection')).toBe('VENDOR_SELECTION');
    expect(moduleRequiredForPath('/bom')).toBe('PRODUCT_COSTING');
  });
});
