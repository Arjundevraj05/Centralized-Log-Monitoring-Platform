import { Copy, Download, Play, Radio, Search, Square, Terminal } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { getErrorMessage, logApi, serverApi } from '../api/client';
import EmptyState from '../components/EmptyState';
import { useAuth } from '../context/AuthContext';
import { useLogStream } from '../hooks/useLogStream';
import type { LogStreamMessage, LogType, Server } from '../types';
import { toStreamCommandKey } from '../utils/jwt';

type Tab = 'fetch' | 'search' | 'stream';

function classifyLine(line: string): string {
  if (line.startsWith('[ERROR]') || /\bERROR\b/i.test(line)) return 'error';
  if (line.includes('Stream')) return 'end';
  if (/\bWARN(ING)?\b/i.test(line)) return 'warn';
  return '';
}

export default function LogsPage() {
  const { hasRole, token } = useAuth();
  const canSearch = hasRole('ADMIN', 'DEV');
  const [tab, setTab] = useState<Tab>('fetch');

  const [servers, setServers] = useState<Server[]>([]);
  const [logTypes, setLogTypes] = useState<LogType[]>([]);
  const [serverId, setServerId] = useState<number | ''>('');
  const [commandKey, setCommandKey] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [lines, setLines] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const [streaming, setStreaming] = useState(false);
  const streamIdRef = useRef(`stream-${Date.now()}`);
  const logViewerRef = useRef<HTMLDivElement>(null);
  const { startStream, stopStream, disconnect } = useLogStream(token);

  useEffect(() => {
    Promise.all([serverApi.list(), logApi.getTypes()])
      .then(([s, t]) => {
        const active = s.filter((sv) => sv.active);
        setServers(active);
        setLogTypes(t);
        if (active.length > 0) setServerId(active[0].id);
        if (t.length > 0) setCommandKey(t[0].commandKey);
      })
      .catch((err) => setError(getErrorMessage(err)));
  }, []);

  useEffect(() => {
    if (logViewerRef.current) {
      logViewerRef.current.scrollTop = logViewerRef.current.scrollHeight;
    }
  }, [lines]);

  useEffect(() => () => disconnect(), [disconnect]);

  const handleFetch = async () => {
    if (!serverId || !commandKey) return;
    setLoading(true);
    setError('');
    setLines([]);
    try {
      const result = await logApi.fetch({ serverId, commandKey });
      setLines(result.lines);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!serverId || !commandKey || !searchTerm) return;
    setLoading(true);
    setError('');
    setLines([]);
    try {
      const result = await logApi.search({ serverId, commandKey, searchTerm });
      setLines(result.lines);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleStartStream = () => {
    if (!serverId || !commandKey || !token) return;
    setError('');
    setLines([]);
    setStreaming(true);
    streamIdRef.current = `stream-${Date.now()}`;
    const streamKey = toStreamCommandKey(commandKey);

    startStream(
      serverId,
      streamKey,
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

  const handleCopy = async () => {
    await navigator.clipboard.writeText(lines.join('\n'));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownload = () => {
    const blob = new Blob([lines.join('\n')], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `logs-${Date.now()}.txt`;
    a.click();
    URL.revokeObjectURL(url);
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

      <div className="tabs">
        <button className={`tab${tab === 'fetch' ? ' active' : ''}`} onClick={() => setTab('fetch')}>
          <Download size={16} />
          Fetch Logs
        </button>
        {canSearch && (
          <button className={`tab${tab === 'search' ? ' active' : ''}`} onClick={() => setTab('search')}>
            <Search size={16} />
            Search Logs
          </button>
        )}
        <button className={`tab${tab === 'stream' ? ' active' : ''}`} onClick={() => setTab('stream')}>
          <Radio size={16} />
          Live Stream
        </button>
      </div>

      <div className="card tabs-panel">
        <div className="card-body">
          <div className="form-row">
            <div className="form-group">
              <label>Server</label>
              <select
                className="form-control"
                value={serverId}
                onChange={(e) => setServerId(Number(e.target.value))}
                disabled={servers.length === 0}
              >
                {servers.length === 0 ? (
                  <option value="">No active servers</option>
                ) : (
                  servers.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.serverName} ({s.host})
                    </option>
                  ))
                )}
              </select>
            </div>
            <div className="form-group">
              <label>Log Type</label>
              <select
                className="form-control"
                value={commandKey}
                onChange={(e) => setCommandKey(e.target.value)}
              >
                {logTypes.map((t) => (
                  <option key={t.commandKey} value={t.commandKey}>
                    {t.logName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {tab === 'search' && (
            <div className="form-group">
              <label>Search Term</label>
              <input
                className="form-control"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="e.g. OutOfMemoryError, ERROR, timeout"
              />
              <p className="form-hint">Letters, digits, spaces, dash, dot, underscore only (max 100 chars)</p>
            </div>
          )}

          <div className="toolbar">
            {tab === 'fetch' && (
              <button className="btn btn-primary" onClick={handleFetch} disabled={loading || !serverId}>
                {loading ? <span className="spinner" /> : <Download size={16} />}
                Fetch Logs
              </button>
            )}
            {tab === 'search' && (
              <button
                className="btn btn-primary"
                onClick={handleSearch}
                disabled={loading || !serverId || !searchTerm}
              >
                {loading ? <span className="spinner" /> : <Search size={16} />}
                Search
              </button>
            )}
            {tab === 'stream' && (
              <>
                {!streaming ? (
                  <button className="btn btn-primary" onClick={handleStartStream} disabled={!serverId}>
                    <Play size={16} />
                    Start Stream
                  </button>
                ) : (
                  <>
                    <button className="btn btn-danger" onClick={handleStopStream}>
                      <Square size={16} />
                      Stop Stream
                    </button>
                    <span className="stream-live">
                      <span className="stream-dot" />
                      LIVE
                    </span>
                  </>
                )}
              </>
            )}
            {lines.length > 0 && (
              <span className="toolbar-meta">
                {lines.length} line{lines.length !== 1 ? 's' : ''}
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="card card-flush">
        {lines.length === 0 ? (
          <EmptyState
            icon={Terminal}
            title="No log output yet"
            description="Select a server and log type, then fetch, search, or start a live stream."
          />
        ) : (
          <div className="log-viewer-wrap">
            <div className="log-toolbar">
              <span className="log-toolbar-title">Output</span>
              <div className="log-toolbar-actions">
                <button type="button" className="btn" onClick={handleCopy}>
                  <Copy size={14} />
                  {copied ? 'Copied!' : 'Copy'}
                </button>
                <button type="button" className="btn" onClick={handleDownload}>
                  <Download size={14} />
                  Download
                </button>
                <button type="button" className="btn" onClick={() => setLines([])}>
                  Clear
                </button>
              </div>
            </div>
            <div className="log-viewer" ref={logViewerRef}>
              {lines.map((line, i) => (
                <div key={i} className="log-line-row">
                  <span className="log-line-num">{i + 1}</span>
                  <div className={`log-line ${classifyLine(line)}`}>{line}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
