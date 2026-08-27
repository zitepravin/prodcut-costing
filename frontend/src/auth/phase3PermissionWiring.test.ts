import { describe, expect, it } from 'vitest';
import { hasPermission, PERMISSIONS } from './permissionMatrix';

describe('Phase 3 permission wiring (UI matrix)', () => {
  it('COSTING_ENGINEER can create BOM, not delete RFQ', () => {
    expect(hasPermission(['COSTING_ENGINEER'], PERMISSIONS.BOM_CREATE)).toBe(true);
    expect(hasPermission(['COSTING_ENGINEER'], PERMISSIONS.SHOULD_COST)).toBe(true);
    expect(hasPermission(['COSTING_ENGINEER'], PERMISSIONS.RFQ_DELETE)).toBe(false);
  });

  it('PURCHASING_BUYER can create RFQ, not create BOM', () => {
    expect(hasPermission(['PURCHASING_BUYER'], PERMISSIONS.RFQ_CREATE)).toBe(true);
    expect(hasPermission(['PURCHASING_BUYER'], PERMISSIONS.BOM_CREATE)).toBe(false);
  });

  it('VIEWER is read-only on BOM/RFQ', () => {
    expect(hasPermission(['VIEWER'], PERMISSIONS.BOM_VIEW)).toBe(true);
    expect(hasPermission(['VIEWER'], PERMISSIONS.RFQ_VIEW)).toBe(true);
    expect(hasPermission(['VIEWER'], PERMISSIONS.BOM_CREATE)).toBe(false);
    expect(hasPermission(['VIEWER'], PERMISSIONS.RFQ_CREATE)).toBe(false);
  });
});
