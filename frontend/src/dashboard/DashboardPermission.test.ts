import { describe, expect, it } from 'vitest';
import { personaForRole } from './dashboardPersona';
import { hasPermission, PERMISSIONS } from '../auth/permissionMatrix';

describe('DashboardPermission', () => {
  it('Create RFQ action is gated by RFQ_CREATE', () => {
    const buyer = personaForRole('PURCHASING_BUYER');
    const create = buyer.quickActions.find((a) => a.id === 'rfq');
    expect(create?.permission).toBe('RFQ_CREATE');
    expect(hasPermission(['PURCHASING_BUYER'], PERMISSIONS.RFQ_CREATE)).toBe(true);
    expect(hasPermission(['PROCESS_ENGINEER'], PERMISSIONS.RFQ_CREATE)).toBe(false);
  });

  it('costing engineer cannot create RFQ from persona actions', () => {
    const eng = personaForRole('COSTING_ENGINEER');
    expect(eng.quickActions.some((a) => a.permission === 'RFQ_CREATE')).toBe(false);
  });

  it('support ticket action uses SUPPORT_TICKET_READ', () => {
    const admin = personaForRole('CUSTOMER_ADMIN');
    const support = admin.quickActions.find((a) => a.id === 'csupport');
    expect(support?.permission).toBe('SUPPORT_TICKET_READ');
  });

  it('VIEWER has DASHBOARD_VIEW but APPROVAL_OVERRIDE does not', () => {
    expect(hasPermission(['VIEWER'], PERMISSIONS.DASHBOARD_VIEW)).toBe(true);
    expect(hasPermission(['APPROVAL_OVERRIDE'], PERMISSIONS.DASHBOARD_VIEW)).toBe(false);
  });
});
