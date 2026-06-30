import { Pencil, Plus, Server, Trash2, X } from 'lucide-react';
import { FormEvent, useEffect, useState } from 'react';
import { getErrorMessage, serverApi } from '../api/client';
import EmptyState from '../components/EmptyState';
import { useAuth } from '../context/AuthContext';
import type { Server as ServerType, ServerRequest } from '../types';

const emptyForm: ServerRequest = {
  serverName: '',
  host: '',
  port: 22,
  username: '',
  privateKey: '',
  environment: 'prod',
  active: true,
};

export default function ServersPage() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole('ADMIN');
  const [servers, setServers] = useState<ServerType[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<ServerType | null>(null);
  const [form, setForm] = useState<ServerRequest>(emptyForm);
  const [saving, setSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      setServers(await serverApi.list());
      setError('');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setModalOpen(true);
  };

  const openEdit = (server: ServerType) => {
    setEditing(server);
    setForm({
      serverName: server.serverName,
      host: server.host,
      port: server.port,
      username: server.username,
      environment: server.environment,
      active: server.active,
      privateKey: '',
    });
    setModalOpen(true);
  };

  const handleDelete = async (server: ServerType) => {
    if (!confirm(`Delete server "${server.serverName}"?`)) return;
    try {
      await serverApi.delete(server.id);
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      if (editing) {
        await serverApi.update(editing.id, form);
      } else {
        await serverApi.create(form);
      }
      setModalOpen(false);
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

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

      <div className="card">
        <div className="card-header">
          <h3>SSH Servers ({servers.length})</h3>
          {isAdmin && (
            <button className="btn btn-primary btn-sm" onClick={openCreate}>
              <Plus size={16} />
              Add Server
            </button>
          )}
        </div>

        {loading ? (
          <div className="loading-center">
            <span className="spinner dark" />
            Loading servers…
          </div>
        ) : servers.length === 0 ? (
          <EmptyState
            icon={Server}
            title="No servers registered"
            description="Add an SSH server to start fetching and streaming logs from your infrastructure."
            action={
              isAdmin ? (
                <button className="btn btn-primary" onClick={openCreate}>
                  <Plus size={16} />
                  Add your first server
                </button>
              ) : undefined
            }
          />
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Host</th>
                  <th>Port</th>
                  <th>Username</th>
                  <th>Environment</th>
                  <th>Status</th>
                  {isAdmin && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {servers.map((s) => (
                  <tr key={s.id}>
                    <td>
                      <strong>{s.serverName}</strong>
                    </td>
                    <td className="cell-mono">{s.host}</td>
                    <td className="cell-mono">{s.port}</td>
                    <td>{s.username}</td>
                    <td>
                      <span className="badge badge-env">{s.environment}</span>
                    </td>
                    <td>
                      <span className={`badge ${s.active ? 'badge-active' : 'badge-inactive'}`}>
                        {s.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    {isAdmin && (
                      <td>
                        <div className="cell-actions">
                          <button
                            className="btn btn-secondary btn-icon btn-sm"
                            onClick={() => openEdit(s)}
                            title="Edit server"
                          >
                            <Pencil size={14} />
                          </button>
                          <button
                            className="btn btn-danger btn-icon btn-sm"
                            onClick={() => handleDelete(s)}
                            title="Delete server"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editing ? 'Edit Server' : 'Add Server'}</h3>
              <button className="btn btn-ghost btn-icon btn-sm" onClick={() => setModalOpen(false)}>
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="form-row">
                  <div className="form-group">
                    <label>Server Name</label>
                    <input
                      className="form-control"
                      value={form.serverName}
                      onChange={(e) => setForm({ ...form, serverName: e.target.value })}
                      placeholder="prod-tomcat-01"
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>Environment</label>
                    <select
                      className="form-control"
                      value={form.environment}
                      onChange={(e) => setForm({ ...form, environment: e.target.value })}
                    >
                      <option value="prod">Production</option>
                      <option value="uat">UAT</option>
                      <option value="dev">Development</option>
                      <option value="staging">Staging</option>
                      <option value="local">Local</option>
                    </select>
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Host</label>
                    <input
                      className="form-control"
                      value={form.host}
                      onChange={(e) => setForm({ ...form, host: e.target.value })}
                      placeholder="10.0.1.50"
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>Port</label>
                    <input
                      className="form-control"
                      type="number"
                      value={form.port}
                      onChange={(e) => setForm({ ...form, port: Number(e.target.value) })}
                      required
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label>SSH Username</label>
                  <input
                    className="form-control"
                    value={form.username}
                    onChange={(e) => setForm({ ...form, username: e.target.value })}
                    placeholder="deploy"
                    required
                  />
                </div>
                <div className="form-group">
                  <label>
                    SSH Private Key (RSA PEM)
                    {editing && ' — leave blank to keep existing'}
                  </label>
                  <textarea
                    className="form-control"
                    value={form.privateKey}
                    onChange={(e) => setForm({ ...form, privateKey: e.target.value })}
                    placeholder="-----BEGIN RSA PRIVATE KEY-----"
                    required={!editing}
                    rows={6}
                  />
                  <p className="form-hint">Use RSA PEM format. OpenSSH ed25519 keys are not supported.</p>
                </div>
                <div className="form-group">
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={form.active}
                      onChange={(e) => setForm({ ...form, active: e.target.checked })}
                    />
                    Server is active
                  </label>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? <span className="spinner" /> : editing ? 'Update Server' : 'Create Server'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
