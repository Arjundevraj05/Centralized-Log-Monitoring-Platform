import { Activity, ArrowRight, ScrollText, Server, Shield } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { auditApi, logApi, serverApi } from '../api/client';
import { useAuth } from '../context/AuthContext';

const quickActions = [
  {
    to: '/logs',
    icon: ScrollText,
    iconClass: 'blue',
    title: 'Explore Logs',
    description: 'Fetch recent log lines from any registered server',
  },
  {
    to: '/servers',
    icon: Server,
    iconClass: 'navy',
    title: 'Manage Servers',
    description: 'View and configure SSH log source connections',
  },
  {
    to: '/logs',
    icon: Activity,
    iconClass: 'green',
    title: 'Live Stream',
    description: 'Watch logs in real time via WebSocket tail',
  },
  {
    to: '/audit',
    icon: Shield,
    iconClass: 'purple',
    title: 'Audit Trail',
    description: 'Review who accessed logs and when',
    adminOnly: true,
  },
];

export default function DashboardPage() {
  const { hasRole, user } = useAuth();
  const [serverCount, setServerCount] = useState(0);
  const [logTypeCount, setLogTypeCount] = useState(0);
  const [auditCount, setAuditCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const [servers, logTypes] = await Promise.all([serverApi.list(), logApi.getTypes()]);
        setServerCount(servers.length);
        setLogTypeCount(logTypes.length);

        if (hasRole('ADMIN')) {
          const audit = await auditApi.list({ page: 0, size: 1 });
          setAuditCount(audit.totalElements);
        }
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [hasRole]);

  if (loading) {
    return (
      <div className="loading-center">
        <span className="spinner dark" />
        Loading dashboard…
      </div>
    );
  }

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';

  return (
    <div>
      <div className="welcome-banner">
        <h2>
          {greeting}, {user?.username}
        </h2>
        <p>
          Monitor application logs across your infrastructure. You have {serverCount} server
          {serverCount !== 1 ? 's' : ''} registered and {logTypeCount} log type
          {logTypeCount !== 1 ? 's' : ''} available.
        </p>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon blue">
            <Server size={22} />
          </div>
          <div>
            <div className="stat-value">{serverCount}</div>
            <div className="stat-label">Registered Servers</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon navy">
            <ScrollText size={22} />
          </div>
          <div>
            <div className="stat-value">{logTypeCount}</div>
            <div className="stat-label">Log Types Available</div>
          </div>
        </div>
        {hasRole('ADMIN') && (
          <div className="stat-card">
            <div className="stat-icon green">
              <Shield size={22} />
            </div>
            <div>
              <div className="stat-value">{auditCount}</div>
              <div className="stat-label">Audit Events</div>
            </div>
          </div>
        )}
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Quick Actions</h3>
        </div>
        <div className="card-body">
          <div className="action-grid">
            {quickActions
              .filter((a) => !a.adminOnly || hasRole('ADMIN'))
              .map(({ to, icon: Icon, iconClass, title, description }) => (
                <Link key={title} to={to} className="action-card">
                  <div className={`action-card-icon ${iconClass}`}>
                    <Icon size={22} />
                  </div>
                  <h4>{title}</h4>
                  <p>{description}</p>
                  <ArrowRight size={16} style={{ color: 'var(--paytm-blue)', marginTop: 'auto' }} />
                </Link>
              ))}
          </div>
        </div>
      </div>
    </div>
  );
}
