import { describe, expect, it } from 'vitest';
import {
  ALL_BUSINESS_ROLES,
  personaForRole,
  resolvePersona,
} from './dashboardPersona';

describe('dashboardPersona', () => {
  it('does not create a persona for APPROVAL_OVERRIDE', () => {
    const p = resolvePersona(['APPROVAL_OVERRIDE']);
    expect(p.role).toBe('VIEWER');
    expect(p.id).toBe('OPERATIONS');
  });

  it('prefers SYSTEM_ADMINISTRATOR over COSTING_ENGINEER', () => {
    const p = resolvePersona(['COSTING_ENGINEER', 'SYSTEM_ADMINISTRATOR']);
    expect(p.id).toBe('SYSTEM_ADMINISTRATION');
  });

  it('maps each business role to a dedicated persona', () => {
    const expected: Record<string, string> = {
      SYSTEM_ADMINISTRATOR: 'SYSTEM_ADMINISTRATION',
      MASTER_DATA_ADMIN: 'MASTER_DATA',
      COSTING_ENGINEER: 'COST_ENGINEERING',
      COSTING_MANAGER: 'COST_MANAGEMENT',
      FINANCE_CONTROLLER: 'FINANCE',
      PURCHASING_BUYER: 'PURCHASING_OPERATIONS',
      SOURCING_LEAD: 'SOURCING_MANAGEMENT',
      PROGRAM_COST_MANAGER: 'PROGRAM_COSTING',
      PROCESS_ENGINEER: 'PROCESS_ENGINEERING',
      TOOLING_ENGINEER: 'TOOLING',
      COMMODITY_MATERIAL_BUYER: 'MATERIAL_PROCUREMENT',
      INDUSTRIAL_ENGINEER: 'INDUSTRIAL_ENGINEERING',
      PRODUCT_COST_ENGINEER: 'PRODUCT_COSTING',
      PURCHASING_MANAGER: 'PURCHASING_MANAGEMENT',
      PLATFORM_OWNER: 'PLATFORM_ADMINISTRATION',
      CUSTOMER_ADMIN: 'CUSTOMER_ADMINISTRATION',
    };
    expect(ALL_BUSINESS_ROLES).toHaveLength(16);
    for (const role of ALL_BUSINESS_ROLES) {
      expect(personaForRole(role).id).toBe(expected[role]);
    }
  });

  it('normalizes ROLE_ prefix', () => {
    expect(resolvePersona(['ROLE_COSTING_MANAGER']).id).toBe('COST_MANAGEMENT');
  });

  it('buyer does not get manager-only approve purchase as primary persona', () => {
    const buyer = personaForRole('PURCHASING_BUYER');
    const manager = personaForRole('PURCHASING_MANAGER');
    expect(buyer.id).not.toBe(manager.id);
    expect(buyer.quickActions.some((a) => a.id === 'rfq')).toBe(true);
    expect(manager.quickActions.some((a) => a.id === 'appr')).toBe(true);
  });

  it('buyer persona includes operational RFQ/PO widgets', () => {
    const buyer = personaForRole('PURCHASING_BUYER');
    expect(buyer.widgets).toEqual(expect.arrayContaining([
      'openRfqs', 'rfqsClosingSoon', 'awaitingQuotations', 'openPurchaseOrders', 'overdueOrders', 'buyerWorkload',
    ]));
    expect(buyer.widgets).not.toContain('purchasingSpend');
  });

  it('purchasing manager includes spend and not create-rfq as contained primary', () => {
    const manager = personaForRole('PURCHASING_MANAGER');
    expect(manager.widgets).toEqual(expect.arrayContaining([
      'openRfqs', 'openPurchaseOrders', 'overdueOrders', 'purchasingSpend', 'buyerWorkload',
    ]));
  });

  it('process and industrial personas expose routing KPIs', () => {
    expect(personaForRole('PROCESS_ENGINEER').widgets).toEqual(
      expect.arrayContaining(['draftRoutings', 'routingExceptions', 'cycleTimeIssues']),
    );
    expect(personaForRole('INDUSTRIAL_ENGINEER').widgets).toEqual(
      expect.arrayContaining(['draftRoutings', 'cycleTimeIssues']),
    );
  });

  it('tooling persona exposes investment widget', () => {
    expect(personaForRole('TOOLING_ENGINEER').widgets).toContain('toolingInvestment');
  });

  it('master data and product cost expose incomplete materials', () => {
    expect(personaForRole('MASTER_DATA_ADMIN').widgets).toContain('incompleteMasterData');
    expect(personaForRole('PRODUCT_COST_ENGINEER').widgets).toContain('incompleteMasterData');
  });
});
