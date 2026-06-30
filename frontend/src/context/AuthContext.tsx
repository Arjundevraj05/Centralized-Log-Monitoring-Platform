import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { authApi, tokenStorage } from '../api/client';
import type { AuthUser, Role } from '../types';
import { isTokenExpired, parseJwt } from '../utils/jwt';

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => tokenStorage.get());
  const [user, setUser] = useState<AuthUser | null>(() => {
    const t = tokenStorage.get();
    return t && !isTokenExpired(t) ? parseJwt(t) : null;
  });
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (token && isTokenExpired(token)) {
      tokenStorage.clear();
      setToken(null);
      setUser(null);
    }
  }, [token]);

  const login = useCallback(async (username: string, password: string) => {
    setIsLoading(true);
    try {
      const { accessToken } = await authApi.login({ username, password });
      tokenStorage.set(accessToken);
      setToken(accessToken);
      setUser(parseJwt(accessToken));
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setToken(null);
    setUser(null);
  }, []);

  const hasRole = useCallback(
    (...roles: Role[]) => {
      if (!user) return false;
      return roles.some((r) => user.roles.includes(r));
    },
    [user]
  );

  const value = useMemo(
    () => ({ user, token, isLoading, login, logout, hasRole }),
    [user, token, isLoading, login, logout, hasRole]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
