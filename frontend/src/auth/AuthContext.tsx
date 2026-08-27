import { hasPermission as matrixHasPermission } from './permissionMatrix';
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as authApi from '../api/authApi';
import { fetchMyAccess } from '../api/platformAccessApi';
import {
  clearSession,
  extractAuthorities,
  getAccessToken,
  getUsername,
  hasSession,
  saveSession,
} from './tokenStorage';
import { AuthContext, type AuthState } from './authContextBase';

function readState(): AuthState {
  const token = getAccessToken();
  return {
    authenticated: hasSession(),
    username: getUsername(),
    authorities: extractAuthorities(token),
    effectiveAccess: null,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(() => readState());
  const [activeCustomerId, setActiveCustomerIdState] = useState<string | null>(() => {
    try {
      return localStorage.getItem('pcs.activeCustomerId');
    } catch {
      return null;
    }
  });

  const setActiveCustomerId = useCallback((customerId: string | null) => {
    try {
      if (customerId) {
        localStorage.setItem('pcs.activeCustomerId', customerId);
      } else {
        localStorage.removeItem('pcs.activeCustomerId');
      }
    } catch {
      /* ignore */
    }
    setActiveCustomerIdState(customerId);
  }, []);

  const refreshAccess = useCallback(async () => {
    try {
      const access = await fetchMyAccess();
      setState((s) => ({ ...s, effectiveAccess: access }));
      try {
        const stored = localStorage.getItem('pcs.activeCustomerId');
        const membershipIds = new Set(access.customers?.map((c) => c.customerId) ?? []);
        if (stored && membershipIds.has(stored)) {
          setActiveCustomerIdState(stored);
        } else {
          const customers = access.customers ?? [];
          const preferred =
            customers.find((c) => c.customerCode === 'DEFAULT') ?? customers[0] ?? null;
          const first = preferred?.customerId ?? null;
          if (first) {
            localStorage.setItem('pcs.activeCustomerId', first);
            setActiveCustomerIdState(first);
          } else {
            localStorage.removeItem('pcs.activeCustomerId');
            setActiveCustomerIdState(null);
          }
        }
      } catch {
        /* ignore */
      }
    } catch {
      setState((s) => ({ ...s, effectiveAccess: null }));
    }
  }, []);

  useEffect(() => {
    if (state.authenticated) {
      void refreshAccess();
    }
  }, [state.authenticated, refreshAccess]);

  const login = useCallback(async (username: string, password: string, mfaCode?: string) => {
    const tokens = await authApi.login({
      username,
      password,
      mfaCode: mfaCode || undefined,
    });
    saveSession(tokens.accessToken, tokens.refreshToken, tokens.expiresInMs, username);
    setState({
      authenticated: true,
      username,
      authorities: extractAuthorities(tokens.accessToken),
      effectiveAccess: null,
    });
    await refreshAccess();
  }, [refreshAccess]);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      clearSession();
      try {
        localStorage.removeItem('pcs.activeCustomerId');
      } catch {
        /* ignore */
      }
      setActiveCustomerIdState(null);
      setState({ authenticated: false, username: null, authorities: [], effectiveAccess: null });
    }
  }, []);

  const hasPermission = useCallback(
    (permissionCode: string) => matrixHasPermission(state.authorities, permissionCode),
    [state.authorities],
  );

  const hasRole = useCallback(
    (...roles: string[]) => {
      const normalized = state.authorities.map((a) => a.replace(/^ROLE_/, ''));
      return roles.some((r) => normalized.includes(r) || state.authorities.includes(r));
    },
    [state.authorities],
  );

  const hasModule = useCallback(
    (moduleCode: string) => {
      if (hasRole('SYSTEM_ADMINISTRATOR', 'ADMIN')) {
        return true;
      }
      const codes =
        state.effectiveAccess?.customers.flatMap((c) => c.enabledModuleCodes) ?? [];
      return codes.includes(moduleCode);
    },
    [state.effectiveAccess, hasRole],
  );

  const value = useMemo(
    () => ({
      ...state,
      login,
      logout,
      hasRole,
      hasPermission,
      refreshAccess,
      hasModule,
      setActiveCustomerId,
      activeCustomerId,
    }),
    [state, login, logout, hasRole, hasPermission, refreshAccess, hasModule, setActiveCustomerId, activeCustomerId],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
