import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../types';

export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { user, token } = useAuth();

  if (!token || !user) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !roles.some((r) => user.roles.includes(r))) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
