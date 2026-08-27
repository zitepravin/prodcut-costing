import { describe, expect, it } from 'vitest';
import { hasPermission, PERMISSIONS, permissionsForRoles } from './permissionMatrix';

describe('permissionMatrix — branding', () => {
  it('CUSTOMER_ADMIN has MANAGE', () => {
    expect(hasPermission(['CUSTOMER_ADMIN'], PERMISSIONS.CUSTOMER_BRANDING_MANAGE)).toBe(true);
    expect(hasPermission(['ROLE_CUSTOMER_ADMIN'], PERMISSIONS.CUSTOMER_BRANDING_MANAGE)).toBe(true);
  });

  it('COSTING_MANAGER denied MANAGE', () => {
    expect(hasPermission(['COSTING_MANAGER'], PERMISSIONS.CUSTOMER_BRANDING_MANAGE)).toBe(false);
  });
});

describe('permissionMatrix — support tickets', () => {
  it('customer roles can create and reply', () => {
    for (const role of ['CUSTOMER_ADMIN', 'COSTING_MANAGER', 'PURCHASING_BUYER', 'VIEWER']) {
      expect(hasPermission([role], PERMISSIONS.SUPPORT_TICKET_READ)).toBe(true);
      expect(hasPermission([role], PERMISSIONS.SUPPORT_TICKET_CREATE)).toBe(true);
      expect(hasPermission([role], PERMISSIONS.SUPPORT_TICKET_REPLY)).toBe(true);
      expect(hasPermission([role], PERMISSIONS.SUPPORT_TICKET_CLOSE)).toBe(true);
    }
  });

  it('customer roles cannot MANAGE platform tickets', () => {
    expect(hasPermission(['CUSTOMER_ADMIN'], PERMISSIONS.SUPPORT_TICKET_MANAGE)).toBe(false);
    expect(hasPermission(['COSTING_ENGINEER'], PERMISSIONS.SUPPORT_TICKET_MANAGE)).toBe(false);
  });

  it('SYSTEM_ADMINISTRATOR can MANAGE', () => {
    expect(hasPermission(['SYSTEM_ADMINISTRATOR'], PERMISSIONS.SUPPORT_TICKET_MANAGE)).toBe(true);
    expect(hasPermission(['ADMIN'], PERMISSIONS.SUPPORT_TICKET_MANAGE)).toBe(true);
  });

  it('empty authorities denied', () => {
    expect(hasPermission([], PERMISSIONS.SUPPORT_TICKET_READ)).toBe(false);
    expect(hasPermission(null, PERMISSIONS.SUPPORT_TICKET_CREATE)).toBe(false);
  });

  it('permissionsForRoles unions grants', () => {
    const perms = permissionsForRoles(['VIEWER']);
    expect(perms).toContain(PERMISSIONS.SUPPORT_TICKET_READ);
    expect(perms).not.toContain(PERMISSIONS.SUPPORT_TICKET_MANAGE);
  });
});

describe('permissionMatrix — extended business roles', () => {
  it('COMMODITY_MATERIAL_BUYER can RFQ create, not BOM create', () => {
    expect(hasPermission(['COMMODITY_MATERIAL_BUYER'], PERMISSIONS.RFQ_CREATE)).toBe(true);
    expect(hasPermission(['COMMODITY_MATERIAL_BUYER'], PERMISSIONS.BOM_CREATE)).toBe(false);
  });

  it('PURCHASING_MANAGER can approve costing; PURCHASING_BUYER cannot', () => {
    expect(hasPermission(['PURCHASING_MANAGER'], PERMISSIONS.COSTING_APPROVE)).toBe(true);
    expect(hasPermission(['PURCHASING_BUYER'], PERMISSIONS.COSTING_APPROVE)).toBe(false);
  });

  it('APPROVAL_OVERRIDE grants no dashboard permissions', () => {
    expect(hasPermission(['APPROVAL_OVERRIDE'], PERMISSIONS.DASHBOARD_VIEW)).toBe(false);
    expect(hasPermission(['APPROVAL_OVERRIDE'], PERMISSIONS.COSTING_APPROVE)).toBe(false);
  });
});
