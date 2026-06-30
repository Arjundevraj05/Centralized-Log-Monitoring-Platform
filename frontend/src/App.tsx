import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AuthProvider } from './context/AuthContext';
import ApplicationLogsPage from './pages/ApplicationLogsPage';
import AuditPage from './pages/AuditPage';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import LogsPage from './pages/LogsPage';
import ServersPage from './pages/ServersPage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/servers" element={<ServersPage />} />
              <Route path="/logs" element={<LogsPage />} />
              <Route path="/app-logs" element={<ApplicationLogsPage />} />
              <Route element={<ProtectedRoute roles={['ADMIN']} />}>
                <Route path="/audit" element={<AuditPage />} />
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
