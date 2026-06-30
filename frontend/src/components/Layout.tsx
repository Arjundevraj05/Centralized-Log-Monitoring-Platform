import { Layers, LogOut, Menu, ScrollText, Server, LayoutDashboard, Shield, X } from 'lucide-react';
import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard', roles: ['ADMIN', 'DEV', 'SUPPORT'] as const },
  { to: '/servers', icon: Server, label: 'Servers', roles: ['ADMIN', 'DEV', 'SUPPORT'] as const },
  { to: '/logs', icon: ScrollText, label: 'Server Logs', roles: ['ADMIN', 'DEV', 'SUPPORT'] as const },
  { to: '/app-logs', icon: Layers, label: 'Application Logs', roles: ['ADMIN', 'DEV', 'SUPPORT'] as const },
  { to: '/audit', icon: Shield, label: 'Audit Trail', roles: ['ADMIN'] as const },
];

const pageMeta: Record<string, { title: string; subtitle: string }> = {
  '/': { title: 'Dashboard', subtitle: 'Overview of your monitoring environment' },
  '/servers': { title: 'Server Management', subtitle: 'Register and manage SSH log sources' },
  '/logs': { title: 'Log Explorer', subtitle: 'Fetch, search, and stream server-level logs' },
  '/app-logs': { title: 'Application Logs', subtitle: 'Tomcat → application → logback-based log viewing' },
  '/audit': { title: 'Audit Trail', subtitle: 'Security and compliance activity log' },
};

export default function Layout() {
  const { user, logout, hasRole } = useAuth();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const meta = pageMeta[location.pathname] ?? { title: 'Paytm Log Monitor', subtitle: '' };
  const initials = user?.username?.slice(0, 2).toUpperCase() ?? '?';

  const closeSidebar = () => setSidebarOpen(false);

  return (
    <div className="app-layout">
      <div
        className={`sidebar-overlay${sidebarOpen ? ' visible' : ''}`}
        onClick={closeSidebar}
        aria-hidden="true"
      />

      <aside className={`sidebar${sidebarOpen ? ' open' : ''}`}>
        <div className="sidebar-brand">
          <div className="sidebar-brand-mark">
            <img src="/paytm-icon.svg" alt="" />
          </div>
          <div className="sidebar-brand-text">
            <h1>Paytm</h1>
            <span>Log Monitoring</span>
          </div>
        </div>

        <nav className="sidebar-nav">
          <span className="nav-section-label">Navigation</span>
          {navItems
            .filter((item) => item.roles.some((r) => hasRole(r)))
            .map(({ to, icon: Icon, label }) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/'}
                className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
                onClick={closeSidebar}
              >
                <span className="nav-link-icon">
                  <Icon size={18} />
                </span>
                {label}
              </NavLink>
            ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-card">
            <div className="user-avatar">{initials}</div>
            <div className="user-details">
              <strong>{user?.username}</strong>
              <span>Signed in</span>
              <div className="user-roles">
                {user?.roles.map((r) => (
                  <span key={r} className={`badge badge-${r.toLowerCase()}`}>
                    {r}
                  </span>
                ))}
              </div>
            </div>
          </div>
          <button className="btn btn-sidebar btn-sm" onClick={logout}>
            <LogOut size={14} />
            Sign out
          </button>
        </div>
      </aside>

      <div className="main-content">
        <header className="topbar">
          <div className="topbar-left">
            <button
              type="button"
              className="menu-toggle"
              onClick={() => setSidebarOpen((o) => !o)}
              aria-label={sidebarOpen ? 'Close menu' : 'Open menu'}
            >
              {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
            <div>
              <h2>{meta.title}</h2>
              {meta.subtitle && <p className="topbar-subtitle">{meta.subtitle}</p>}
            </div>
          </div>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
