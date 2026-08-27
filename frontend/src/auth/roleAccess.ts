/**
 * Frontend access matrix for navigation and route guards.
 * Backend @RequiresModule + module entitlement remain the security boundary.
 * This only controls visibility / early UX denial — never grant access the API denies.
 *
 * Wave A: strict module separation — PURCHASE and VENDOR_SELECTION are not Product Costing paths.
 */

export type AccessCheck = {
  hasRole: (...roles: string[]) => boolean;
  hasModule: (moduleCode: string) => boolean;
};

/** Paths that require at least one of these roles (any match grants nav/route). */
export const PATH_REQUIRED_ROLES: Record<string, readonly string[]> = {
  '/customer-admin': ['CUSTOMER_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR'],
  '/customer-admin/users': ['CUSTOMER_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR'],
  '/customer-admin/departments': ['CUSTOMER_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR'],

  '/audit': ['ADMIN', 'SYSTEM_ADMINISTRATOR', 'CUSTOMER_ADMIN'],
  '/platform': ['ADMIN', 'SYSTEM_ADMINISTRATOR', 'PLATFORM_OWNER'],
  '/platform/customers': ['ADMIN', 'SYSTEM_ADMINISTRATOR', 'PLATFORM_OWNER'],
  '/platform/modules': ['ADMIN', 'SYSTEM_ADMINISTRATOR', 'PLATFORM_OWNER'],
  '/platform/configuration': ['ADMIN', 'SYSTEM_ADMINISTRATOR', 'PLATFORM_OWNER'],
  '/platform/users': ['ADMIN', 'SYSTEM_ADMINISTRATOR'],
  '/platform/roles': ['ADMIN', 'SYSTEM_ADMINISTRATOR'],
  '/platform/billing': ['ADMIN', 'SYSTEM_ADMINISTRATOR', 'PLATFORM_OWNER'],
  '/platform/support/tickets': ['ADMIN', 'SYSTEM_ADMINISTRATOR', 'PLATFORM_OWNER'],
  '/platform/entitlements': ['ADMIN', 'SYSTEM_ADMINISTRATOR'],
  '/customer-admin/role-permissions': ['CUSTOMER_ADMIN', 'ADMIN', 'SYSTEM_ADMINISTRATOR'],
  '/platform/permissions': ['ADMIN', 'SYSTEM_ADMINISTRATOR', 'PLATFORM_OWNER', 'CUSTOMER_ADMIN'],
  '/platform/access-audit': ['ADMIN', 'SYSTEM_ADMINISTRATOR', 'PLATFORM_OWNER'],
};

/**
 * Explicit path → module entitlement.
 * Checked before PRODUCT_COSTING path list.
 */
export const PATH_REQUIRED_MODULE: Record<string, string> = {
  '/vendor-selection': 'VENDOR_SELECTION',
  '/purchase-requests': 'PURCHASE',
  '/purchase-orders': 'PURCHASE',
};

/** Product Costing only — never list PURCHASE or VENDOR_SELECTION here. */
export const PRODUCT_COSTING_PATHS: readonly string[] = [
  '/master-data',
  '/product-costing',
  '/sourcing',
  '/operations',
  '/materials',
  '/bom',
  '/should-cost',
  '/universal-costing',
  '/import-costing',
  '/costing-variants',
  '/quantity-structure',
  '/cost-structure',
  '/costing-sheets',
  '/advanced-costing',
  '/scenarios',
  '/procurement-alternatives',
  '/standard-costs',
  '/financial',
  '/mfg-intel',
  '/auto-intel',
  '/supplier-portal',
  '/lifecycle',
  '/mixed-costing',
  '/make-vs-buy',
  '/approvals',
  '/machines',
  '/process',
  '/tooling',
  '/packaging',
  '/target-costing',
  '/rfq',
  '/cost-comparison',
  '/import-export',
  '/currency',
  '/benchmarks',
  '/business-case',
  '/erp-staging',
  '/sap-cost-estimates',
  '/versions',
  '/reports',
  '/notifications',
];

/**
 * Resolve whether the current user may see/navigate to a path.
 * Order: role gate → explicit module gate → product costing set → allow.
 */
export function canAccessPath(path: string, check: AccessCheck): boolean {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  const roleEntry = Object.entries(PATH_REQUIRED_ROLES)
    .filter(([p]) => normalized === p || normalized.startsWith(p + '/'))
    .sort((a, b) => b[0].length - a[0].length)[0];
  if (roleEntry) {
    return check.hasRole(...roleEntry[1]);
  }
  const moduleCode = PATH_REQUIRED_MODULE[normalized];
  if (moduleCode) {
    return check.hasModule(moduleCode);
  }
  if (PRODUCT_COSTING_PATHS.includes(normalized)) {
    return check.hasModule('PRODUCT_COSTING');
  }
  return true;
}

export function rolesRequiredForPath(path: string): readonly string[] | null {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  const roleEntry = Object.entries(PATH_REQUIRED_ROLES)
    .filter(([p]) => normalized === p || normalized.startsWith(p + '/'))
    .sort((a, b) => b[0].length - a[0].length)[0];
  return roleEntry ? roleEntry[1] : null;
}

/** Module code required for path, if any. */
export function moduleRequiredForPath(path: string): string | null {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  if (PATH_REQUIRED_MODULE[normalized]) {
    return PATH_REQUIRED_MODULE[normalized];
  }
  if (PRODUCT_COSTING_PATHS.includes(normalized)) {
    return 'PRODUCT_COSTING';
  }
  return null;
}
