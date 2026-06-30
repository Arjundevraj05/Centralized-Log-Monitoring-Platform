export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
}

export interface Server {
  id: number;
  serverName: string;
  host: string;
  port: number;
  username: string;
  environment: string;
  active: boolean;
  createdAt: string;
}

export interface ServerRequest {
  serverName: string;
  host: string;
  port: number;
  username: string;
  privateKey?: string;
  environment: string;
  active: boolean;
}

export interface LogType {
  logName: string;
  commandKey: string;
}

export interface LogResponse {
  serverId: number;
  commandKey: string;
  lines: string[];
  lineCount: number;
}

export interface LogFetchRequest {
  serverId: number;
  commandKey: string;
}

export interface LogSearchRequest {
  serverId: number;
  commandKey: string;
  searchTerm: string;
}

export interface LogStreamMessage {
  streamId: string;
  serverId: number;
  commandKey: string;
  line?: string;
  type: 'LOG' | 'ERROR' | 'END';
  timestamp: string;
}

export interface AuditLog {
  id: number;
  username: string;
  action: string;
  resource: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AppLogFetchRequest {
  logConfigId: number;
  mode: 'CURRENT' | 'ARCHIVED';
  logDate?: string;
}

export interface TomcatInstance {
  id: number;
  serverId: number;
  instanceName: string;
  catalinaHome: string;
  discoveredAt: string;
}

export interface TomcatApplication {
  id: number;
  tomcatInstanceId: number;
  appName: string;
  logConfigCached: boolean;
  discoveredAt: string;
}

export interface ApplicationLogConfig {
  id: number;
  applicationId: number;
  currentLogPath: string;
  archivedPathPattern?: string;
  refreshedAt: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  message: string;
  path: string;
}

export type Role = 'ADMIN' | 'DEV' | 'SUPPORT';

export interface AuthUser {
  username: string;
  roles: Role[];
}
