import { describe, expect, it } from 'vitest';
import { ALL_BUSINESS_ROLES, personaForRole, resolvePersona } from './dashboardPersona';
import { hasPermission, PERMISSIONS } from '../auth/permissionMatrix';

const ROLES = [...ALL_BUSINESS_ROLES];

describe('DashboardRole — 16 business roles', () => {
  it.each(ROLES)('%s resolves a non-fallback persona with DASHBOARD_VIEW', (role) => {
    const p = personaForRole(role);
    expect(p.id).not.toBe('OPERATIONS');
    expect(p.role).toBe(role);
    expect(p.widgets.length).toBeGreaterThan(0);
    expect(hasPermission([role], PERMISSIONS.DASHBOARD_VIEW)).toBe(true);
  });

  it('APPROVAL_OVERRIDE is not a dashboard persona and has no DASHBOARD_VIEW', () => {
    expect(resolvePersona(['APPROVAL_OVERRIDE']).id).toBe('OPERATIONS');
    expect(hasPermission(['APPROVAL_OVERRIDE'], PERMISSIONS.DASHBOARD_VIEW)).toBe(false);
  });

  it('multi-role prefers platform owner over buyer', () => {
    expect(resolvePersona(['PURCHASING_BUYER', 'PLATFORM_OWNER']).role).toBe('PLATFORM_OWNER');
  });
});
