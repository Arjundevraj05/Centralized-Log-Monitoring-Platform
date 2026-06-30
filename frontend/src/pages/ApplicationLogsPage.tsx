import {
  Calendar,
  Download,
  Layers,
  Play,
  Radio,
  RefreshCw,
  Square,
  Terminal,
} from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { appLogApi, getErrorMessage, serverApi, tomcatApi } from '../api/client';
import EmptyState from '../components/EmptyState';
import { useAuth } from '../context/AuthContext';
import { useLogStream } from '../hooks/useLogStream';
import type {
  ApplicationLogConfig,
  LogStreamMessage,
  Server,
  TomcatApplication,
  TomcatInstance,
} from '../types';

type LogMode = 'CURRENT' | 'ARCHIVED' | 'LIVE';

export default function ApplicationLogsPage() {
  const { hasRole, token } = useAuth();
  const canDiscover = hasRole('ADMIN', 'DEV');
  const { startAppStream, stopStream, disconnect } = useLogStream(token);

  const [servers, setServers] = useState<Server[]>([]);
  const [serverId, setServerId] = useState<number | ''>('');
  const [instances, setInstances] = useState<TomcatInstance[]>([]);
  const [instanceId, setInstanceId] = useState<number | ''>('');
  const [applications, setApplications] = useState<TomcatApplication[]>([]);
  const [applicationId, setApplicationId] = useState<number | ''>('');
  const [logConfig, setLogConfig] = useState<ApplicationLogConfig | null>(null);
  const [logMode, setLogMode] = useState<LogMode>('CURRENT');
  const [logDate, setLogDate] = useState('');
  const [lines, setLines] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [streaming, setStreaming] = useState(false);
  const [error, setError] = useState('');
  const streamIdRef = useRef(`app-stream-${Date.now()}`);
  const logViewerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    serverApi
      .list()
      .then((s) => {
        const active = s.filter((sv) => sv.active);
        setServers(active);
        if (active.length > 0) setServerId(active[0].id);
      })
      .catch((err) => setError(getErrorMessage(err)));
  }, []);

  useEffect(() => {
    if (!serverId) return;
    tomcatApi
      .listInstances(serverId)
      .then(setInstances)
      .catch((err) => setError(getErrorMessage(err)));
    setInstanceId('');
    setApplicationId('');
    setApplications([]);
    setLogConfig(null);
    setLines([]);
  }, [serverId]);

  useEffect(() => {
    if (!serverId || !instanceId || !applicationId) return;
    const app = applications.find((a) => a.id === applicationId);
    if (app?.logConfigCached) {
      tomcatApi
        .getLogConfig(serverId, instanceId, applicationId)
        .then(setLogConfig)
        .catch(() => setLogConfig(null));
    } else {
      setLogConfig(null);
    }
  }, [serverId, instanceId, applicationId, applications]);

  useEffect(() => {
    if (logViewerRef.current) {
      logViewerRef.current.scrollTop = logViewerRef.current.scrollHeight;
    }
  }, [lines]);

  useEffect(() => () => disconnect(), [disconnect]);

  const handleDiscoverInstances = async () => {
    if (!serverId) return;
    setLoading(true);
    setError('');
    try {
      const found = await tomcatApi.discoverInstances(serverId);
      setInstances(found);
      if (found.length > 0) setInstanceId(found[0].id);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleDiscoverApps = async () => {
    if (!serverId || !instanceId) return;
    setLoading(true);
    setError('');
    try {
      const found = await tomcatApi.discoverApplications(serverId, instanceId);
      setApplications(found);
      if (found.length > 0) setApplicationId(found[0].id);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleCacheLogConfig = async () => {
    if (!serverId || !instanceId || !applicationId) return;
    setLoading(true);
    setError('');
    try {
      const config = await tomcatApi.cacheLogConfig(serverId, instanceId, applicationId);
      setLogConfig(config);
      setApplications((prev) =>
        prev.map((a) => (a.id === applicationId ? { ...a, logConfigCached: true } : a))
      );
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleLoadLogs = async () => {
    if (!logConfig) return;
    if (logMode === 'LIVE') return;

    setLoading(true);
    setError('');
    setLines([]);
    try {
      const result = await appLogApi.fetch({
        logConfigId: logConfig.id,
        mode: logMode,
        logDate: logMode === 'ARCHIVED' ? logDate : undefined,
      });
      setLines(result.lines);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleStartStream = () => {
    if (!logConfig) return;
    setError('');
    setLines([]);
    setStreaming(true);
    streamIdRef.current = `app-stream-${Date.now()}`;

    startAppStream(
      logConfig.id,
      streamIdRef.current,
      (msg: LogStreamMessage) => {
        if (msg.type === 'LOG' && msg.line) {
          setLines((prev) => [...prev, msg.line!]);
        } else if (msg.type === 'ERROR' && msg.line) {
          setLines((prev) => [...prev, `[ERROR] ${msg.line}`]);
        } else if (msg.type === 'END') {
          setLines((prev) => [...prev, '--- Stream ended ---']);
          setStreaming(false);
        }
      },
      (err) => {
        setError(err);
        setStreaming(false);
      }
    );
  };

  const handleStopStream = () => {
    stopStream(streamIdRef.current);
    setStreaming(false);
    setLines((prev) => [...prev, '--- Stream stopped ---']);
  };

  const selectedInstance = instances.find((i) => i.id === instanceId);
  const selectedApp = applications.find((a) => a.id === applicationId);

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
          <h3>1. Server & Tomcat</h3>
        </div>
        <div className="card-body">
          <div className="form-row">
            <div className="form-group">
              <label>SSH Server</label>
              <select
                className="form-control"
                value={serverId}
                onChange={(e) => setServerId(Number(e.target.value))}
              >
                {servers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.serverName} ({s.host})
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Tomcat Instance</label>
              <select
                className="form-control"
                value={instanceId}
                onChange={(e) => setInstanceId(Number(e.target.value))}
                disabled={instances.length === 0}
              >
                {instances.length === 0 ? (
                  <option value="">No instances — discover first</option>
                ) : (
                  instances.map((i) => (
                    <option key={i.id} value={i.id}>
                      {i.instanceName}
                    </option>
                  ))
                )}
              </select>
              {selectedInstance && (
                <p className="form-hint">{selectedInstance.catalinaHome}</p>
              )}
            </div>
          </div>
          {canDiscover && (
            <button className="btn btn-secondary btn-sm" onClick={handleDiscoverInstances} disabled={loading || !serverId}>
              <RefreshCw size={14} />
              Discover Tomcat instances (~/local/apache-tomcat-*)
            </button>
          )}
        </div>
      </div>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div className="card-header">
          <h3>2. Application</h3>
        </div>
        <div className="card-body">
          <div className="form-group">
            <label>Deployed application (webapps)</label>
            <select
              className="form-control"
              value={applicationId}
              onChange={(e) => {
                setApplicationId(Number(e.target.value));
                setLogConfig(null);
              }}
              disabled={!instanceId || applications.length === 0}
            >
              {applications.length === 0 ? (
                <option value="">No applications — discover first</option>
              ) : (
                applications.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.appName} {a.logConfigCached ? '✓' : ''}
                  </option>
                ))
              )}
            </select>
          </div>
          {canDiscover && (
            <div className="toolbar">
              <button
                className="btn btn-secondary btn-sm"
                onClick={handleDiscoverApps}
                disabled={loading || !instanceId}
              >
                <Layers size={14} />
                Discover applications
              </button>
              <button
                className="btn btn-primary btn-sm"
                onClick={handleCacheLogConfig}
                disabled={loading || !applicationId}
              >
                <Terminal size={14} />
                Load logback.xml
              </button>
            </div>
          )}
          {selectedApp && !selectedApp.logConfigCached && (
            <p className="form-hint" style={{ marginTop: '0.75rem' }}>
              Reads WEB-INF/classes/logback.xml and caches log paths in the database.
            </p>
          )}
        </div>
      </div>

      {logConfig && (
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div className="card-header">
            <h3>3. View logs</h3>
          </div>
          <div className="card-body">
            <div className="form-group">
              <label>Current log path</label>
              <input className="form-control cell-mono" readOnly value={logConfig.currentLogPath} />
            </div>
            {logConfig.archivedPathPattern && (
              <div className="form-group">
                <label>Archived pattern</label>
                <input className="form-control cell-mono" readOnly value={logConfig.archivedPathPattern} />
              </div>
            )}

            <div className="tabs" style={{ marginTop: '1rem' }}>
              <button
                className={`tab${logMode === 'CURRENT' ? ' active' : ''}`}
                onClick={() => setLogMode('CURRENT')}
              >
                Current logs
              </button>
              {logConfig.archivedPathPattern && (
                <button
                  className={`tab${logMode === 'ARCHIVED' ? ' active' : ''}`}
                  onClick={() => setLogMode('ARCHIVED')}
                >
                  <Calendar size={14} />
                  Date-wise
                </button>
              )}
              <button
                className={`tab${logMode === 'LIVE' ? ' active' : ''}`}
                onClick={() => setLogMode('LIVE')}
              >
                <Radio size={14} />
                Live stream
              </button>
            </div>

            {logMode === 'ARCHIVED' && (
              <div className="form-group">
                <label>Log date</label>
                <input
                  type="date"
                  className="form-control"
                  value={logDate}
                  onChange={(e) => setLogDate(e.target.value)}
                />
              </div>
            )}

            <div className="toolbar">
              {logMode === 'LIVE' ? (
                !streaming ? (
                  <button className="btn btn-primary" onClick={handleStartStream}>
                    <Play size={16} />
                    Start live tail
                  </button>
                ) : (
                  <>
                    <button className="btn btn-danger" onClick={handleStopStream}>
                      <Square size={16} />
                      Stop
                    </button>
                    <span className="stream-live">
                      <span className="stream-dot" />
                      LIVE
                    </span>
                  </>
                )
              ) : (
                <button
                  className="btn btn-primary"
                  onClick={handleLoadLogs}
                  disabled={loading || (logMode === 'ARCHIVED' && !logDate)}
                >
                  {loading ? <span className="spinner" /> : <Download size={16} />}
                  Load logs
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      <div className="card card-flush">
        {lines.length === 0 ? (
          <EmptyState
            icon={Terminal}
            title="No application log output"
            description="Discover Tomcat, select an application, load logback.xml, then fetch or stream logs."
          />
        ) : (
          <div className="log-viewer-wrap">
            <div className="log-toolbar">
              <span className="log-toolbar-title">Output ({lines.length} lines)</span>
              <button type="button" className="btn" onClick={() => setLines([])}>
                Clear
              </button>
            </div>
            <div className="log-viewer" ref={logViewerRef}>
              {lines.map((line, i) => (
                <div key={i} className="log-line-row">
                  <span className="log-line-num">{i + 1}</span>
                  <div className="log-line">{line}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
