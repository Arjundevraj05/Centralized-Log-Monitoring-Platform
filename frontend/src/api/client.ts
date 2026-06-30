import axios, { type AxiosError } from 'axios';
import type {
  ApiError,
  AppLogFetchRequest,
  ApplicationLogConfig,
  AuditLog,
  LogFetchRequest,
  LogResponse,
  LogSearchRequest,
  LogType,
  LoginRequest,
  LoginResponse,
  PageResponse,
  Server,
  ServerRequest,
  TomcatApplication,
  TomcatInstance,
} from '../types';

const TOKEN_KEY = 'paytm_log_monitor_token';

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      tokenStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error)) {
    return error.response?.data?.message ?? error.message;
  }
  if (error instanceof Error) return error.message;
  return 'An unexpected error occurred';
}

export const authApi = {
  login: (data: LoginRequest) =>
    api.post<LoginResponse>('/auth/login', data).then((r) => r.data),
};

export const serverApi = {
  list: () => api.get<Server[]>('/servers').then((r) => r.data),
  get: (id: number) => api.get<Server>(`/servers/${id}`).then((r) => r.data),
  create: (data: ServerRequest) => api.post<Server>('/servers', data).then((r) => r.data),
  update: (id: number, data: ServerRequest) =>
    api.put<Server>(`/servers/${id}`, data).then((r) => r.data),
  delete: (id: number) => api.delete(`/servers/${id}`),
};

export const logApi = {
  getTypes: () => api.get<LogType[]>('/log-types').then((r) => r.data),
  fetch: (data: LogFetchRequest) =>
    api.post<LogResponse>('/logs/fetch', data).then((r) => r.data),
  search: (data: LogSearchRequest) =>
    api.post<LogResponse>('/logs/search', data).then((r) => r.data),
};

export const tomcatApi = {
  listInstances: (serverId: number) =>
    api.get<TomcatInstance[]>(`/servers/${serverId}/tomcat/instances`).then((r) => r.data),
  discoverInstances: (serverId: number) =>
    api.post<TomcatInstance[]>(`/servers/${serverId}/tomcat/instances/discover`).then((r) => r.data),
  listApplications: (serverId: number, instanceId: number) =>
    api
      .get<TomcatApplication[]>(`/servers/${serverId}/tomcat/instances/${instanceId}/applications`)
      .then((r) => r.data),
  discoverApplications: (serverId: number, instanceId: number) =>
    api
      .post<TomcatApplication[]>(`/servers/${serverId}/tomcat/instances/${instanceId}/applications/discover`)
      .then((r) => r.data),
  cacheLogConfig: (serverId: number, instanceId: number, applicationId: number) =>
    api
      .post<ApplicationLogConfig>(
        `/servers/${serverId}/tomcat/instances/${instanceId}/applications/${applicationId}/log-config/cache`
      )
      .then((r) => r.data),
  getLogConfig: (serverId: number, instanceId: number, applicationId: number) =>
    api
      .get<ApplicationLogConfig>(
        `/servers/${serverId}/tomcat/instances/${instanceId}/applications/${applicationId}/log-config`
      )
      .then((r) => r.data),
};

export const appLogApi = {
  fetch: (data: AppLogFetchRequest) =>
    api.post<LogResponse>('/app-logs/fetch', data).then((r) => r.data),
};

export const auditApi = {
  list: (params?: { page?: number; size?: number; username?: string; action?: string }) =>
    api.get<PageResponse<AuditLog>>('/audit', { params }).then((r) => r.data),
};

export default api;
