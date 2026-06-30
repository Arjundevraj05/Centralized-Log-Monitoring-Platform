import type { AuthUser, Role } from '../types';

interface JwtPayload {
  sub: string;
  roles?: string[];
  exp: number;
}

export function parseJwt(token: string): AuthUser | null {
  try {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as JwtPayload;
    const roles = (decoded.roles ?? [])
      .map((r) => r.replace('ROLE_', '') as Role)
      .filter((r): r is Role => ['ADMIN', 'DEV', 'SUPPORT'].includes(r));
    return { username: decoded.sub, roles };
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  try {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as JwtPayload;
    return decoded.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

export function hasRole(user: AuthUser | null, ...roles: Role[]): boolean {
  if (!user) return false;
  return roles.some((r) => user.roles.includes(r));
}

export function toStreamCommandKey(fetchKey: string): string {
  if (fetchKey.endsWith('_TAIL')) return fetchKey;
  if (fetchKey.endsWith('_LOG')) return fetchKey.replace(/_LOG$/, '_TAIL');
  return `${fetchKey}_TAIL`;
}
