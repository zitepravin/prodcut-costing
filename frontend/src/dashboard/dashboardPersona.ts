/**
 * Role → dashboard persona mapping.
 * One shell, permission-aware widgets. APPROVAL_OVERRIDE is not a persona.
 */

export type DashboardPersonaId =
  | 'SYSTEM_ADMINISTRATION'
  | 'PLATFORM_ADMINISTRATION'
  | 'CUSTOMER_ADMINISTRATION'
  | 'MASTER_DATA'
  | 'COST_ENGINEERING'
  | 'COST_MANAGEMENT'
  | 'FINANCE'
  | 'PURCHASING_OPERATIONS'
  | 'SOURCING_MANAGEMENT'
  | 'PROGRAM_COSTING'
  | 'PROCESS_ENGINEERING'
  | 'TOOLING'
  | 'MATERIAL_PROCUREMENT'
  | 'INDUSTRIAL_ENGINEERING'
  | 'PRODUCT_COSTING'
  | 'PURCHASING_MANAGEMENT'
  | 'OPERATIONS';

export type WidgetId =
  | 'programCost'
  | 'targetCost'
  | 'variance'
  | 'shouldCostHighlight'
  | 'overallStatus'
  | 'programs'
  | 'materials'
  | 'bom'
  | 'shouldCost'
  | 'targets'
  | 'packaging'
  | 'approvals'
  | 'notifications'
  | 'vendorRecs'
  | 'vendorRisks'
  | 'vendorWarranty'
  | 'vendorCaps'
  | 'support'
  | 'platformAdmin'
  | 'customerAdmin'
  | 'openRfqs'
  | 'rfqsClosingSoon'
  | 'awaitingQuotations'
  | 'openPurchaseOrders'
  | 'overdueOrders'
  | 'openPurchaseRequests'
  | 'purchasingSpend'
  | 'buyerWorkload'
  | 'incompleteMasterData'
  | 'draftRoutings'
  | 'routingExceptions'
  | 'cycleTimeIssues'
  | 'toolingInvestment';

export interface QuickActionDef {
  id: string;
  label: string;
  to: string;
  permission?: string;
  module?: string;
  roles?: readonly string[];
  variant?: 'contained' | 'outlined' | 'secondary';
}

export interface WorkspaceLink {
  label: string;
  to: string;
  permission?: string;
  module?: string;
}

export interface DashboardPersona {
  id: DashboardPersonaId;
  role: string;
  title: string;
  purpose: string;
  widgets: readonly WidgetId[];
  workspaces: readonly WorkspaceLink[];
  quickActions: readonly QuickActionDef[];
}

/** First matching role wins. APPROVAL_OVERRIDE is skipped. */
export const PERSONA_PRIORITY: readonly string[] = [
  'PLATFORM_OWNER',
  'SYSTEM_ADMINISTRATOR',
  'ADMIN',
  'CUSTOMER_ADMIN',
  'COSTING_MANAGER',
  'PROGRAM_COST_MANAGER',
  'PURCHASING_MANAGER',
  'SOURCING_LEAD',
  'COSTING_ENGINEER',
  'PRODUCT_COST_ENGINEER',
  'FINANCE_CONTROLLER',
  'MASTER_DATA_ADMIN',
  'PURCHASING_BUYER',
  'COMMODITY_MATERIAL_BUYER',
  'PROCESS_ENGINEER',
  'INDUSTRIAL_ENGINEER',
  'TOOLING_ENGINEER',
];

const COST_WORKSPACES: WorkspaceLink[] = [
  { label: 'Materials', to: '/materials', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
  { label: 'Process', to: '/process', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
  { label: 'Tooling', to: '/tooling', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
  { label: 'Packaging', to: '/packaging', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
  { label: 'Should-Cost', to: '/should-cost', permission: 'SHOULD_COST', module: 'PRODUCT_COSTING' },
  { label: 'Target Costing', to: '/target-costing', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
  { label: 'Cost Comparison', to: '/cost-comparison', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
];

const PERSONAS: Record<string, DashboardPersona> = {
  PLATFORM_OWNER: {
    id: 'PLATFORM_ADMINISTRATION',
    role: 'PLATFORM_OWNER',
    title: 'Platform administration',
    purpose: 'Tenant, subscription, module and platform-level control. Business costing data is not shown unless separately authorized.',
    widgets: ['platformAdmin', 'support', 'notifications', 'overallStatus'],
    workspaces: [],
    quickActions: [
      { id: 'customers', label: 'Customers', to: '/platform/customers', roles: ['PLATFORM_OWNER', 'SYSTEM_ADMINISTRATOR', 'ADMIN'], variant: 'contained' },
      { id: 'modules', label: 'Modules', to: '/platform/modules', roles: ['PLATFORM_OWNER', 'SYSTEM_ADMINISTRATOR', 'ADMIN'] },
      { id: 'billing', label: 'Subscriptions', to: '/platform/billing', roles: ['PLATFORM_OWNER', 'SYSTEM_ADMINISTRATOR', 'ADMIN'] },
      { id: 'config', label: 'Platform configuration', to: '/platform/configuration', roles: ['PLATFORM_OWNER', 'SYSTEM_ADMINISTRATOR', 'ADMIN'] },
      { id: 'psupport', label: 'Support', to: '/platform/support/tickets', roles: ['PLATFORM_OWNER', 'SYSTEM_ADMINISTRATOR', 'ADMIN'] },
    ],
  },
  SYSTEM_ADMINISTRATOR: {
    id: 'SYSTEM_ADMINISTRATION',
    role: 'SYSTEM_ADMINISTRATOR',
    title: 'System administration',
    purpose: 'Full system administration visibility — customers, users, roles, modules, support and audit.',
    widgets: ['platformAdmin', 'programs', 'shouldCost', 'approvals', 'support', 'notifications', 'overallStatus', 'incompleteMasterData'],
    workspaces: COST_WORKSPACES,
    quickActions: [
      { id: 'users', label: 'Users', to: '/platform/users', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'], variant: 'contained' },
      { id: 'roles', label: 'Roles', to: '/platform/roles', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'] },
      { id: 'perms', label: 'Permissions', to: '/platform/permissions', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'] },
      { id: 'customers', label: 'Customers', to: '/platform/customers', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'] },
      { id: 'audit', label: 'Audit', to: '/audit', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'] },
      { id: 'platform', label: 'Platform home', to: '/platform', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'] },
    ],
  },
  ADMIN: {
    id: 'SYSTEM_ADMINISTRATION',
    role: 'ADMIN',
    title: 'System administration',
    purpose: 'Full system administration visibility — customers, users, roles, modules, support and audit.',
    widgets: ['platformAdmin', 'programs', 'shouldCost', 'approvals', 'support', 'notifications', 'overallStatus', 'incompleteMasterData'],
    workspaces: COST_WORKSPACES,
    quickActions: [
      { id: 'users', label: 'Users', to: '/platform/users', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'], variant: 'contained' },
      { id: 'roles', label: 'Roles', to: '/platform/roles', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'] },
      { id: 'perms', label: 'Permissions', to: '/platform/permissions', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'] },
      { id: 'customers', label: 'Customers', to: '/platform/customers', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'] },
      { id: 'audit', label: 'Audit', to: '/audit', roles: ['SYSTEM_ADMINISTRATOR', 'ADMIN'] },
    ],
  },
  CUSTOMER_ADMIN: {
    id: 'CUSTOMER_ADMINISTRATION',
    role: 'CUSTOMER_ADMIN',
    title: 'Customer administration',
    purpose: 'Manage this tenant only — users, roles, entitlements, support and configuration. Other customers are never visible.',
    widgets: ['customerAdmin', 'approvals', 'support', 'notifications', 'overallStatus'],
    workspaces: [],
    quickActions: [
      { id: 'cusers', label: 'Users', to: '/customer-admin/users', roles: ['CUSTOMER_ADMIN'], variant: 'contained' },
      { id: 'croles', label: 'Role permissions', to: '/customer-admin/role-permissions', roles: ['CUSTOMER_ADMIN'] },
      { id: 'csupport', label: 'Support tickets', to: '/support/my-tickets', permission: 'SUPPORT_TICKET_READ' },
      { id: 'branding', label: 'Configuration', to: '/customer-admin', roles: ['CUSTOMER_ADMIN'] },
    ],
  },
  MASTER_DATA_ADMIN: {
    id: 'MASTER_DATA',
    role: 'MASTER_DATA_ADMIN',
    title: 'Master data',
    purpose: 'Maintain products, materials, currencies and other masters. Incomplete records appear as pending work, not invented counts.',
    widgets: ['materials', 'bom', 'programs', 'notifications', 'support', 'overallStatus', 'incompleteMasterData'],
    workspaces: [
      { label: 'Materials', to: '/materials', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
      { label: 'Master Data', to: '/master-data', module: 'PRODUCT_COSTING' },
      { label: 'Currency', to: '/currency', module: 'PRODUCT_COSTING' },
      { label: 'Machines', to: '/machines', module: 'PRODUCT_COSTING' },
    ],
    quickActions: [
      { id: 'mat', label: 'Materials', to: '/materials', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING', variant: 'contained' },
      { id: 'md', label: 'Manage master data', to: '/master-data', module: 'PRODUCT_COSTING' },
      { id: 'cur', label: 'Currency', to: '/currency', module: 'PRODUCT_COSTING' },
    ],
  },
  COSTING_ENGINEER: {
    id: 'COST_ENGINEERING',
    role: 'COSTING_ENGINEER',
    title: 'Cost engineering',
    purpose: 'Build BOMs, routings and should-cost estimates. Workload is taken from live tenant counts.',
    widgets: ['shouldCostHighlight', 'bom', 'shouldCost', 'programs', 'materials', 'packaging', 'approvals', 'support', 'overallStatus', 'incompleteMasterData', 'draftRoutings'],
    workspaces: COST_WORKSPACES,
    quickActions: [
      { id: 'sc', label: 'Create / run costing', to: '/should-cost', permission: 'SHOULD_COST', module: 'PRODUCT_COSTING', variant: 'contained' },
      { id: 'bom', label: 'Create / edit BOM', to: '/bom', permission: 'BOM_CREATE', module: 'PRODUCT_COSTING' },
      { id: 'proc', label: 'Create / edit routing', to: '/process', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
    ],
  },
  COSTING_MANAGER: {
    id: 'COST_MANAGEMENT',
    role: 'COSTING_MANAGER',
    title: 'Cost management',
    purpose: 'Review and approve cost estimates. Variance and pending approvals are live tenant data.',
    widgets: ['variance', 'targetCost', 'shouldCost', 'approvals', 'programs', 'support', 'notifications', 'overallStatus'],
    workspaces: COST_WORKSPACES,
    quickActions: [
      { id: 'appr', label: 'Review costing', to: '/approvals', permission: 'COSTING_APPROVE', variant: 'contained' },
      { id: 'sc', label: 'Should-Cost', to: '/should-cost', permission: 'SHOULD_COST', module: 'PRODUCT_COSTING' },
      { id: 'cmp', label: 'Cost analysis', to: '/cost-comparison', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
      { id: 'tgt', label: 'Target costing', to: '/target-costing', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
    ],
  },
  FINANCE_CONTROLLER: {
    id: 'FINANCE',
    role: 'FINANCE_CONTROLLER',
    title: 'Finance',
    purpose: 'Control rates, currency, overhead and financial costing assumptions. Configuration screens open only when authorized.',
    widgets: ['approvals', 'variance', 'shouldCost', 'notifications', 'support', 'overallStatus'],
    workspaces: [
      { label: 'Currency rates', to: '/currency', module: 'PRODUCT_COSTING' },
      { label: 'Machines / rates', to: '/machines', module: 'PRODUCT_COSTING' },
      { label: 'Financial integration', to: '/financial', module: 'PRODUCT_COSTING' },
      { label: 'Should-Cost', to: '/should-cost', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
    ],
    quickActions: [
      { id: 'cur', label: 'Currency rates', to: '/currency', module: 'PRODUCT_COSTING', variant: 'contained' },
      { id: 'fin', label: 'Overhead / financial', to: '/financial', module: 'PRODUCT_COSTING' },
      { id: 'mac', label: 'Machine rates', to: '/machines', module: 'PRODUCT_COSTING' },
      { id: 'appr', label: 'Review costing', to: '/approvals', permission: 'COSTING_APPROVE' },
    ],
  },
  PURCHASING_BUYER: {
    id: 'PURCHASING_OPERATIONS',
    role: 'PURCHASING_BUYER',
    title: 'Purchasing operations',
    purpose: 'Daily RFQ, quotation and purchasing work — what needs action today.',
    widgets: ['approvals', 'notifications', 'vendorRisks', 'vendorRecs', 'support', 'overallStatus', 'openRfqs', 'rfqsClosingSoon', 'awaitingQuotations', 'openPurchaseOrders', 'overdueOrders', 'openPurchaseRequests', 'buyerWorkload'],
    workspaces: [
      { label: 'RFQ / Quotes', to: '/rfq', permission: 'RFQ_VIEW', module: 'PRODUCT_COSTING' },
      { label: 'Purchase requests', to: '/purchase-requests', module: 'PURCHASE' },
      { label: 'Purchase orders', to: '/purchase-orders', module: 'PURCHASE' },
      { label: 'Vendor selection', to: '/vendor-selection', module: 'VENDOR_SELECTION' },
    ],
    quickActions: [
      { id: 'rfq', label: 'Create RFQ', to: '/rfq', permission: 'RFQ_CREATE', module: 'PRODUCT_COSTING', variant: 'contained' },
      { id: 'rev', label: 'Review RFQ', to: '/rfq', permission: 'RFQ_VIEW', module: 'PRODUCT_COSTING' },
      { id: 'po', label: 'Purchase orders', to: '/purchase-orders', module: 'PURCHASE' },
      { id: 'vs', label: 'Supplier follow-up', to: '/vendor-selection', module: 'VENDOR_SELECTION' },
    ],
  },
  SOURCING_LEAD: {
    id: 'SOURCING_MANAGEMENT',
    role: 'SOURCING_LEAD',
    title: 'Sourcing management',
    purpose: 'Cross-supplier sourcing decisions and award management. Buyer-only create actions appear only with permission.',
    widgets: ['vendorRecs', 'vendorRisks', 'vendorWarranty', 'vendorCaps', 'approvals', 'shouldCost', 'support', 'overallStatus', 'openRfqs', 'awaitingQuotations', 'rfqsClosingSoon'],
    workspaces: [
      { label: 'Vendor selection', to: '/vendor-selection', module: 'VENDOR_SELECTION' },
      { label: 'RFQ / Quotes', to: '/rfq', permission: 'RFQ_VIEW', module: 'PRODUCT_COSTING' },
      { label: 'Should-Cost', to: '/should-cost', permission: 'SHOULD_COST', module: 'PRODUCT_COSTING' },
      { label: 'Cost comparison', to: '/cost-comparison', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
    ],
    quickActions: [
      { id: 'vs', label: 'Compare suppliers', to: '/vendor-selection', module: 'VENDOR_SELECTION', variant: 'contained' },
      { id: 'rfq', label: 'Review award / RFQ', to: '/rfq', permission: 'RFQ_VIEW', module: 'PRODUCT_COSTING' },
      { id: 'sc', label: 'Sourcing analysis', to: '/cost-comparison', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
    ],
  },
  PROGRAM_COST_MANAGER: {
    id: 'PROGRAM_COSTING',
    role: 'PROGRAM_COST_MANAGER',
    title: 'Program costing',
    purpose: 'Program-level target costing and VA/VE. Totals stay NOT AVAILABLE until a program is selected — never invented.',
    widgets: ['programCost', 'targetCost', 'variance', 'programs', 'shouldCost', 'approvals', 'support', 'overallStatus'],
    workspaces: COST_WORKSPACES,
    quickActions: [
      { id: 'tgt', label: 'Target cost', to: '/target-costing', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING', variant: 'contained' },
      { id: 'sc', label: 'Program cost review', to: '/should-cost', permission: 'SHOULD_COST', module: 'PRODUCT_COSTING' },
      { id: 'mb', label: 'Make vs buy / VA-VE', to: '/make-vs-buy', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
    ],
  },
  PROCESS_ENGINEER: {
    id: 'PROCESS_ENGINEERING',
    role: 'PROCESS_ENGINEER',
    title: 'Process engineering',
    purpose: 'Process definition, routings and cycle-time engineering.',
    widgets: ['bom', 'shouldCost', 'programs', 'notifications', 'support', 'overallStatus', 'draftRoutings', 'routingExceptions', 'cycleTimeIssues'],
    workspaces: [
      { label: 'Process / routing', to: '/process', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
      { label: 'BOM', to: '/bom', permission: 'BOM_VIEW', module: 'PRODUCT_COSTING' },
      { label: 'Should-Cost', to: '/should-cost', permission: 'SHOULD_COST', module: 'PRODUCT_COSTING' },
    ],
    quickActions: [
      { id: 'proc', label: 'Create routing', to: '/process', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING', variant: 'contained' },
      { id: 'bom', label: 'Review routing / BOM', to: '/bom', permission: 'BOM_VIEW', module: 'PRODUCT_COSTING' },
    ],
  },
  TOOLING_ENGINEER: {
    id: 'TOOLING',
    role: 'TOOLING_ENGINEER',
    title: 'Tooling',
    purpose: 'Tooling investment and tooling cost estimates.',
    widgets: ['shouldCost', 'bom', 'notifications', 'support', 'overallStatus', 'toolingInvestment'],
    workspaces: [
      { label: 'Tooling', to: '/tooling', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
      { label: 'Should-Cost', to: '/should-cost', permission: 'SHOULD_COST', module: 'PRODUCT_COSTING' },
      { label: 'BOM', to: '/bom', permission: 'BOM_VIEW', module: 'PRODUCT_COSTING' },
    ],
    quickActions: [
      { id: 'tool', label: 'Create tooling estimate', to: '/tooling', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING', variant: 'contained' },
      { id: 'rev', label: 'Review tooling', to: '/tooling', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
    ],
  },
  COMMODITY_MATERIAL_BUYER: {
    id: 'MATERIAL_PROCUREMENT',
    role: 'COMMODITY_MATERIAL_BUYER',
    title: 'Material procurement',
    purpose: 'Material sourcing and material cost control.',
    widgets: ['materials', 'approvals', 'notifications', 'support', 'overallStatus', 'openRfqs', 'incompleteMasterData'],
    workspaces: [
      { label: 'Materials', to: '/materials', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
      { label: 'RFQ / Quotes', to: '/rfq', permission: 'RFQ_VIEW', module: 'PRODUCT_COSTING' },
      { label: 'Currency', to: '/currency', module: 'PRODUCT_COSTING' },
    ],
    quickActions: [
      { id: 'mat', label: 'Material price', to: '/materials', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING', variant: 'contained' },
      { id: 'rfq', label: 'Material RFQ', to: '/rfq', permission: 'RFQ_CREATE', module: 'PRODUCT_COSTING' },
      { id: 'rev', label: 'Material cost review', to: '/materials', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
    ],
  },
  INDUSTRIAL_ENGINEER: {
    id: 'INDUSTRIAL_ENGINEERING',
    role: 'INDUSTRIAL_ENGINEER',
    title: 'Industrial engineering',
    purpose: 'Process routing, cycle time and line balancing.',
    widgets: ['bom', 'shouldCost', 'programs', 'notifications', 'support', 'overallStatus', 'draftRoutings', 'cycleTimeIssues'],
    workspaces: [
      { label: 'Process / routing', to: '/process', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
      { label: 'BOM', to: '/bom', permission: 'BOM_VIEW', module: 'PRODUCT_COSTING' },
    ],
    quickActions: [
      { id: 'proc', label: 'Routing', to: '/process', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING', variant: 'contained' },
      { id: 'bom', label: 'Process review', to: '/bom', permission: 'BOM_VIEW', module: 'PRODUCT_COSTING' },
    ],
  },
  PRODUCT_COST_ENGINEER: {
    id: 'PRODUCT_COSTING',
    role: 'PRODUCT_COST_ENGINEER',
    title: 'Product costing',
    purpose: 'Build and review should-cost and challenge supplier quotations.',
    widgets: ['programCost', 'shouldCostHighlight', 'bom', 'shouldCost', 'materials', 'variance', 'support', 'overallStatus', 'incompleteMasterData'],
    workspaces: COST_WORKSPACES,
    quickActions: [
      { id: 'sc', label: 'Should-Cost', to: '/should-cost', permission: 'SHOULD_COST', module: 'PRODUCT_COSTING', variant: 'contained' },
      { id: 'bom', label: 'Review BOM cost', to: '/bom', permission: 'BOM_VIEW', module: 'PRODUCT_COSTING' },
      { id: 'cmp', label: 'Compare supplier quote', to: '/cost-comparison', permission: 'COSTING_VIEW', module: 'PRODUCT_COSTING' },
    ],
  },
  PURCHASING_MANAGER: {
    id: 'PURCHASING_MANAGEMENT',
    role: 'PURCHASING_MANAGER',
    title: 'Purchasing management',
    purpose: 'Purchasing approvals, spend oversight and buyer workload — not day-to-day RFQ execution.',
    widgets: ['approvals', 'vendorRecs', 'vendorRisks', 'notifications', 'support', 'overallStatus', 'openRfqs', 'openPurchaseOrders', 'overdueOrders', 'purchasingSpend', 'buyerWorkload'],
    workspaces: [
      { label: 'RFQ / Quotes', to: '/rfq', permission: 'RFQ_VIEW', module: 'PRODUCT_COSTING' },
      { label: 'Purchase orders', to: '/purchase-orders', module: 'PURCHASE' },
      { label: 'Vendor selection', to: '/vendor-selection', module: 'VENDOR_SELECTION' },
      { label: 'Reports', to: '/reports', permission: 'COSTING_VIEW' },
    ],
    quickActions: [
      { id: 'appr', label: 'Approve purchase', to: '/approvals', variant: 'contained' },
      { id: 'rfq', label: 'Review RFQ', to: '/rfq', permission: 'RFQ_VIEW', module: 'PRODUCT_COSTING' },
      { id: 'vs', label: 'Supplier performance', to: '/vendor-selection', module: 'VENDOR_SELECTION' },
      { id: 'rep', label: 'Purchasing reports', to: '/reports' },
    ],
  },
};

const FALLBACK: DashboardPersona = {
  id: 'OPERATIONS',
  role: 'VIEWER',
  title: 'Operations dashboard',
  purpose: 'Permission-aware operational snapshot. Widgets appear only when entitled.',
  widgets: ['shouldCost', 'bom', 'approvals', 'notifications', 'support', 'overallStatus'],
  workspaces: COST_WORKSPACES,
  quickActions: [
    { id: 'sc', label: 'Should-Cost', to: '/should-cost', permission: 'SHOULD_COST', module: 'PRODUCT_COSTING' },
    { id: 'appr', label: 'Approvals', to: '/approvals' },
  ],
};

export function normalizeRole(role: string): string {
  return role.startsWith('ROLE_') ? role.slice(5) : role;
}

export function resolvePersona(authorities: string[] | undefined | null): DashboardPersona {
  const roles = new Set((authorities ?? []).map(normalizeRole).filter((r) => r !== 'APPROVAL_OVERRIDE'));
  for (const code of PERSONA_PRIORITY) {
    if (roles.has(code) && PERSONAS[code]) {
      return PERSONAS[code];
    }
  }
  return FALLBACK;
}

export function personaForRole(roleCode: string): DashboardPersona {
  const code = normalizeRole(roleCode);
  if (code === 'APPROVAL_OVERRIDE') {
    return FALLBACK;
  }
  return PERSONAS[code] ?? FALLBACK;
}

export const ALL_BUSINESS_ROLES: readonly string[] = [
  'SYSTEM_ADMINISTRATOR',
  'MASTER_DATA_ADMIN',
  'COSTING_ENGINEER',
  'COSTING_MANAGER',
  'FINANCE_CONTROLLER',
  'PURCHASING_BUYER',
  'SOURCING_LEAD',
  'PROGRAM_COST_MANAGER',
  'PROCESS_ENGINEER',
  'TOOLING_ENGINEER',
  'COMMODITY_MATERIAL_BUYER',
  'INDUSTRIAL_ENGINEER',
  'PRODUCT_COST_ENGINEER',
  'PURCHASING_MANAGER',
  'PLATFORM_OWNER',
  'CUSTOMER_ADMIN',
];
