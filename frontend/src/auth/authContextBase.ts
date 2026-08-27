import { createContext } from 'react';
import type { EffectiveAccess } from '../api/platformAccessApi';

export interface AuthState {
  authenticated: boolean;
  username: string | null;
  authorities: string[];
  effectiveAccess: EffectiveAccess | null;
}

export interface AuthContextValue extends AuthState {
  login: (username: string, password: string, mfaCode?: string) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (...roles: string[]) => boolean;
  hasPermission: (permissionCode: string) => boolean;
  refreshAccess: () => Promise<void>;
  hasModule: (moduleCode: string) => boolean;
  setActiveCustomerId: (customerId: string | null) => void;
  activeCustomerId: string | null;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
