import { Filter, Shield } from 'lucide-react';
import { useEffect, useState } from 'react';
import { auditApi, getErrorMessage } from '../api/client';
import EmptyState from '../components/EmptyState';
import type { AuditLog } from '../types';

const ACTIONS = [
  'USER_LOGIN',
  'LOG_FETCH',
  'LOG_SEARCH',
  'SERVER_CREATE',
  'SERVER_UPDATE',
  'SERVER_DELETE',
  'TOMCAT_DISCOVER',
  'APP_LOG_CONFIG_CACHE',
  'APP_LOG_FETCH',
];

function actionBadgeClass(action: string): string {
  if (action === 'USER_LOGIN') return 'badge-action-login';
  if (action.startsWith('LOG_')) return `badge-action-${action.split('_')[1].toLowerCase()}`;
  if (action.startsWith('SERVER_')) return 'badge-action-server';
  return 'badge-env';
}

export default function AuditPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [username, setUsername] = useState('');
  const [action, setAction] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async (p = page) => {
    setLoading(true);
    setError('');
    try {
      const result = await auditApi.list({
        page: p,
        size: 20,
        username: username || undefined,
        action: action || undefined,
      });
      setLogs(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
      setPage(result.number);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(0);
  }, []);

  const handleFilter = (e: React.FormEvent) => {
    e.preventDefault();
    load(0);
  };

  const formatTime = (ts: string) => new Date(ts).toLocaleString();

  return (
    <div>
      {error && (
        <div className="alert alert-error">
          {error}
          <button type="button" className="alert-dismiss" onClick={() => setError('')} aria-label="Dismiss">
            ×
          </button>
        </div>
      )}

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div className="card-header">
          <h3>Filters</h3>
        </div>
        <div className="card-body">
          <form onSubmit={handleFilter} className="filter-bar">
            <div className="form-group">
              <label>Username</label>
              <input
                className="form-control"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Filter by user"
              />
            </div>
            <div className="form-group">
              <label>Action</label>
              <select className="form-control" value={action} onChange={(e) => setAction(e.target.value)}>
                <option value="">All actions</option>
                {ACTIONS.map((a) => (
                  <option key={a} value={a}>
                    {a.replace(/_/g, ' ')}
                  </option>
                ))}
              </select>
            </div>
            <button type="submit" className="btn btn-primary">
              <Filter size={16} />
              Apply
            </button>
          </form>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Audit Events ({totalElements.toLocaleString()})</h3>
        </div>

        {loading ? (
          <div className="loading-center">
            <span className="spinner dark" />
            Loading audit logs…
          </div>
        ) : logs.length === 0 ? (
          <EmptyState
            icon={Shield}
            title="No audit events found"
            description="Try adjusting your filters or check back after users perform actions in the system."
          />
        ) : (
          <>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>User</th>
                    <th>Action</th>
                    <th>Resource</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((log) => (
                    <tr key={log.id}>
                      <td style={{ whiteSpace: 'nowrap', fontSize: '0.8rem' }}>{formatTime(log.timestamp)}</td>
                      <td>
                        <strong>{log.username}</strong>
                      </td>
                      <td>
                        <span className={`badge ${actionBadgeClass(log.action)}`}>
                          {log.action.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td style={{ fontSize: '0.8rem', color: 'var(--paytm-text-muted)', maxWidth: '320px' }}>
                        {log.resource ?? '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="pagination">
              <span>
                Page {page + 1} of {totalPages || 1} · {totalElements.toLocaleString()} total
              </span>
              <div className="pagination-buttons">
                <button
                  className="btn btn-secondary btn-sm"
                  disabled={page === 0}
                  onClick={() => load(page - 1)}
                >
                  Previous
                </button>
                <button
                  className="btn btn-secondary btn-sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => load(page + 1)}
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
