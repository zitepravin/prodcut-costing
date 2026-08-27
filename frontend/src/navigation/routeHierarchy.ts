/**
 * Global module hierarchy for breadcrumbs and Back navigation.
 *
 * Dashboard
 * ├── Master Data (/master-data)
 * ├── Product Costing (/product-costing)
 * ├── Sourcing (/sourcing)
 * ├── Operations (/operations)
 * └── Overview pages (Notifications, Approvals) — parent Dashboard only
 *
 * Child pages back to their module landing — never always Dashboard.
 */

export interface BreadcrumbItem {
  label: string;
  to?: string;
}

export interface RouteNavMeta {
  parentPath: string | null;
  parentLabel: string | null;
  breadcrumbs: BreadcrumbItem[];
  /** Module landing (e.g. /product-costing) or null */
  modulePath: string | null;
  moduleLabel: string | null;
  isModuleLanding: boolean;
  isDashboard: boolean;
}

export interface ModuleDef {
  path: string;
  label: string;
  children: Array<{ path: string; label: string }>;
}

/** Authoritative module tree — single source for nav meta */
export const MODULES: ModuleDef[] = [
  {
    path: '/master-data',
    label: 'Master Data',
    children: [
      { path: '/materials', label: 'Materials' },
      { path: '/machines', label: 'Machines' },
      { path: '/currency', label: 'Currency' },
      { path: '/import-export', label: 'Import / Export' },
      { path: '/benchmarks', label: 'Benchmark Library' },
    ],
  },
  {
    path: '/product-costing',
    label: 'Product Costing',
    children: [
      { path: '/bom', label: 'BOM' },
      { path: '/process', label: 'Process Costing' },
      { path: '/tooling', label: 'Tooling' },
      { path: '/packaging', label: 'Packaging' },
      { path: '/should-cost', label: 'Should-Cost' },
      { path: '/costing-variants', label: 'Costing Variants' },
      { path: '/quantity-structure', label: 'Quantity Structure' },
      { path: '/cost-structure', label: 'Cost Structure' },
      { path: '/costing-sheets', label: 'Costing Sheets' },
      { path: '/advanced-costing', label: 'Advanced Costing' },
      { path: '/scenarios', label: 'Scenarios' },
      { path: '/procurement-alternatives', label: 'Procurement Alternatives' },
      { path: '/standard-costs', label: 'Standard Costs' },
      { path: '/financial', label: 'Financial Integration' },
      { path: '/mfg-intel', label: 'Mfg Intelligence' },
      { path: '/auto-intel', label: 'Automotive Intel' },
      { path: '/lifecycle', label: 'Lifecycle' },
      { path: '/mixed-costing', label: 'Mixed Costing' },
      { path: '/make-vs-buy', label: 'Make vs Buy' },
      { path: '/target-costing', label: 'Target Costing' },
      { path: '/cost-comparison', label: 'Cost Comparison' },
      { path: '/versions', label: 'Versions' },
      { path: '/business-case', label: 'Business Case' },
    ],
  },
  {
    path: '/purchase',
    label: 'Purchase',
    children: [
      { path: '/purchase-requests', label: 'Purchase Requests' },
      { path: '/purchase-orders', label: 'Purchase Orders' },
    ],
  },
  {
    path: '/sourcing',
    label: 'Sourcing',
    children: [
      { path: '/rfq', label: 'RFQ / Quotes' },
      { path: '/vendor-selection', label: 'Vendor Selection' },
      { path: '/supplier-portal', label: 'Supplier Portal' },
    ],
  },
  {
    path: '/operations',
    label: 'Operations',
    children: [
      { path: '/reports', label: 'Reports' },
      { path: '/erp-staging', label: 'ERP Staging' },
      { path: '/sap-cost-estimates', label: 'SAP Cost Estimates' },
      { path: '/audit', label: 'Audit Log' },
    ],
  },
];

const OVERVIEW: Record<string, string> = {
  '/': 'Dashboard',
  '/notifications': 'Notifications',
  '/approvals': 'Approvals',
};

const PLATFORM: Record<string, string> = {
  '/customer-admin': 'Customer Administration',
  '/customer-admin/users': 'Users',
  '/customer-admin/departments': 'Departments',
  '/customer-admin/license-usage': 'License & Usage',
  '/platform': 'Platform Admin',
  '/platform/customers': 'Customers',
  '/platform/modules': 'Modules',
  '/platform/configuration': 'Configuration',
  '/platform/users': 'Users',
  '/platform/roles': 'Roles',
  '/platform/permissions': 'Permissions',
  '/platform/access-audit': 'Access Audit',
  '/platform/billing': 'Billing',
};

const CHILD_TO_MODULE = new Map<string, ModuleDef>();
const PATH_LABEL = new Map<string, string>();

for (const m of MODULES) {
  PATH_LABEL.set(m.path, m.label);
  for (const c of m.children) {
    CHILD_TO_MODULE.set(c.path, m);
    PATH_LABEL.set(c.path, c.label);
  }
}
for (const [p, l] of Object.entries(OVERVIEW)) PATH_LABEL.set(p, l);
for (const [p, l] of Object.entries(PLATFORM)) PATH_LABEL.set(p, l);

export function normalizePath(pathname: string): string {
  if (!pathname || pathname === '') return '/';
  const p = pathname.endsWith('/') && pathname.length > 1 ? pathname.slice(0, -1) : pathname;
  return p || '/';
}

export function labelFor(path: string): string {
  return PATH_LABEL.get(path) ?? path.split('/').filter(Boolean).pop()?.replace(/-/g, ' ') ?? path;
}

export function resolveRouteNavMeta(pathname: string, _fromPath?: string | null): RouteNavMeta {
  const path = normalizePath(pathname);

  if (path === '/' || path === '/login') {
    return {
      parentPath: null,
      parentLabel: null,
      breadcrumbs: [{ label: 'Dashboard' }],
      modulePath: null,
      moduleLabel: null,
      isModuleLanding: false,
      isDashboard: true,
    };
  }

  // Module landing pages
  const landing = MODULES.find((m) => m.path === path);
  if (landing) {
    return {
      parentPath: '/',
      parentLabel: 'Dashboard',
      breadcrumbs: [
        { label: 'Dashboard', to: '/' },
        { label: landing.label },
      ],
      modulePath: landing.path,
      moduleLabel: landing.label,
      isModuleLanding: true,
      isDashboard: false,
    };
  }

  // Module children
  const mod = CHILD_TO_MODULE.get(path);
  if (mod) {
    const childLabel = labelFor(path);
    return {
      parentPath: mod.path,
      parentLabel: mod.label,
      breadcrumbs: [
        { label: 'Dashboard', to: '/' },
        { label: mod.label, to: mod.path },
        { label: childLabel },
      ],
      modulePath: mod.path,
      moduleLabel: mod.label,
      isModuleLanding: false,
      isDashboard: false,
    };
  }

  // Overview (Notifications, Approvals)
  if (path in OVERVIEW && path !== '/') {
    return {
      parentPath: '/',
      parentLabel: 'Dashboard',
      breadcrumbs: [
        { label: 'Dashboard', to: '/' },
        { label: OVERVIEW[path] },
      ],
      modulePath: null,
      moduleLabel: null,
      isModuleLanding: false,
      isDashboard: false,
    };
  }

  // Platform
  if (path === '/platform') {
    return {
      parentPath: '/',
      parentLabel: 'Dashboard',
      breadcrumbs: [
        { label: 'Dashboard', to: '/' },
        { label: 'Platform Admin' },
      ],
      modulePath: '/platform',
      moduleLabel: 'Platform Admin',
      isModuleLanding: true,
      isDashboard: false,
    };
  }
  if (path.startsWith('/platform/')) {
    return {
      parentPath: '/platform',
      parentLabel: 'Platform Admin',
      breadcrumbs: [
        { label: 'Dashboard', to: '/' },
        { label: 'Platform Admin', to: '/platform' },
        { label: labelFor(path) },
      ],
      modulePath: '/platform',
      moduleLabel: 'Platform Admin',
      isModuleLanding: false,
      isDashboard: false,
    };
  }

  // Fallback
  return {
    parentPath: '/',
    parentLabel: 'Dashboard',
    breadcrumbs: [
      { label: 'Dashboard', to: '/' },
      { label: labelFor(path) },
    ],
    modulePath: null,
    moduleLabel: null,
    isModuleLanding: false,
    isDashboard: false,
  };
}

/** Back shown for module children and nested platform pages; not for pure Dashboard. */
export function shouldShowBackButton(meta: RouteNavMeta): boolean {
  if (meta.isDashboard) return false;
  if (!meta.parentPath || !meta.parentLabel) return false;
  // Module landings: optional Back to Dashboard (legitimate — parent is Dashboard)
  // Children: Back to module landing
  return true;
}

export function backButtonLabel(meta: RouteNavMeta): string {
  return meta.parentLabel ? `Back to ${meta.parentLabel}` : 'Back';
}

export function getModuleForPath(pathname: string): ModuleDef | undefined {
  const path = normalizePath(pathname);
  return MODULES.find((m) => m.path === path) ?? CHILD_TO_MODULE.get(path);
}
