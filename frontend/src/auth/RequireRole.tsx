import { Alert, Box } from '@mui/material';
import type { ReactNode } from 'react';
import { useAuth } from './useAuth';
import { rolesRequiredForPath } from './roleAccess';

type Props = {
  /** At least one role required */
  roles: readonly string[];
  children: ReactNode;
  /** Optional path for message context */
  path?: string;
};

/**
 * Page-level role gate. Does not replace backend security.
 */
export function RequireRole({ roles, children, path }: Props) {
  const { hasRole } = useAuth();
  const allowed = hasRole(...roles);
  if (allowed) {
    return <>{children}</>;
  }
  const label = roles.join(' or ');
  return (
    <Box p={2}>
      <Alert severity="warning">
        {path
          ? `Access to ${path} is restricted to roles: ${label}.`
          : `This feature is restricted to roles: ${label}.`}
      </Alert>
    </Box>
  );
}

/** Convenience: gate by path using roleAccess matrix. */
export function RequirePathAccess({ path, children }: { path: string; children: ReactNode }) {
  const roles = rolesRequiredForPath(path);
  if (!roles) {
    return <>{children}</>;
  }
  return (
    <RequireRole roles={roles} path={path}>
      {children}
    </RequireRole>
  );
}
