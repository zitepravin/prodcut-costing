import { describe, expect, it } from 'vitest';
import { personaForRole } from './dashboardPersona';

describe('DashboardEntitlement', () => {
  it('buyer RFQ actions require PRODUCT_COSTING module', () => {
    const buyer = personaForRole('PURCHASING_BUYER');
    const rfq = buyer.quickActions.find((a) => a.id === 'rfq');
    expect(rfq?.module).toBe('PRODUCT_COSTING');
    const po = buyer.quickActions.find((a) => a.id === 'po');
    expect(po?.module).toBe('PURCHASE');
  });

  it('platform owner has empty costing workspaces (no default business data)', () => {
    expect(personaForRole('PLATFORM_OWNER').workspaces).toHaveLength(0);
  });

  it('costing engineer workspaces are PRODUCT_COSTING module tagged', () => {
    const eng = personaForRole('COSTING_ENGINEER');
    expect(eng.workspaces.every((w) => !w.module || w.module === 'PRODUCT_COSTING')).toBe(true);
  });

  it('customer admin does not link to other-customer platform customers page', () => {
    const ca = personaForRole('CUSTOMER_ADMIN');
    expect(ca.quickActions.some((a) => a.to.startsWith('/platform/customers'))).toBe(false);
    expect(ca.quickActions.some((a) => a.to.startsWith('/customer-admin'))).toBe(true);
  });
});
