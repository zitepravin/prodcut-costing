import { describe, expect, it } from 'vitest';
import { DashboardPage } from './DashboardPage';
import { ALL_BUSINESS_ROLES, personaForRole } from '../dashboard/dashboardPersona';

describe('DashboardPage', () => {
  it('exports a page component', () => {
    expect(typeof DashboardPage).toBe('function');
  });

  it('every business role persona has at least one widget the page can render', () => {
    for (const role of ALL_BUSINESS_ROLES) {
      const p = personaForRole(role);
      expect(p.widgets.length).toBeGreaterThan(0);
      expect(p.widgets).toContain('overallStatus');
    }
  });
});
