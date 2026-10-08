import axios, { AxiosInstance } from "axios";
import type {
  AuthResponse,
  User,
  Project,
  Bug,
  BugSummary,
  Comment,
  BugStats,
  ActivityLog,
  DuplicateCheckResponse,
  Attachment,
  Page,
  BugFilters,
} from "@/types";

const api: AxiosInstance = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (v: string) => void;
  reject: (e: unknown) => void;
}> = [];

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config;
    if (err.response?.status === 401 && !original._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`;
          return api(original);
        });
      }
      original._retry = true;
      isRefreshing = true;
      try {
        const refreshToken = localStorage.getItem("refreshToken");
        if (!refreshToken) throw new Error("No refresh token");
        const { data } = await axios.post<AuthResponse>("/api/auth/refresh", {
          refreshToken,
        });
        localStorage.setItem("accessToken", data.accessToken);
        localStorage.setItem("refreshToken", data.refreshToken);
        failedQueue.forEach((p) => p.resolve(data.accessToken));
        failedQueue = [];
        original.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(original);
      } catch {
        failedQueue.forEach((p) => p.reject(err));
        failedQueue = [];
        localStorage.clear();
        window.location.href = "/login";
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(err);
  },
);

// Auth
export const authApi = {
  register: (data: {
    username: string;
    email: string;
    password: string;
    fullName?: string;
  }) => api.post<AuthResponse>("/auth/register", data),
  login: (data: { username: string; password: string }) =>
    api.post<AuthResponse>("/auth/login", data),
  logout: () => api.post("/auth/logout"),
  me: () => api.get<User>("/auth/me"),
  updateProfile: (data: { fullName?: string; avatarUrl?: string }) =>
    api.put<User>("/auth/me", data),
};

// Projects
export const projectApi = {
  getAll: () => api.get<Project[]>("/projects"),
  getById: (id: string) => api.get<Project>(`/projects/${id}`),
  create: (data: { name: string; key: string; description?: string }) =>
    api.post<Project>("/projects", data),
  update: (
    id: string,
    data: { name?: string; description?: string; status?: string },
  ) => api.put<Project>(`/projects/${id}`, data),
  delete: (id: string) => api.delete(`/projects/${id}`),
};

// Bugs
export const bugApi = {
  getAll: (projectId: string, filters: BugFilters = {}) =>
    api.get<Page<BugSummary>>(`/projects/${projectId}/bugs`, {
      params: filters,
    }),
  getById: (projectId: string, id: string) =>
    api.get<Bug>(`/projects/${projectId}/bugs/${id}`),
  create: (
    projectId: string,
    data: {
      title: string;
      description?: string;
      priority?: string;
      severity?: string;
      assigneeId?: string;
      stepsToReproduce?: string;
      expectedBehavior?: string;
      actualBehavior?: string;
      environment?: string;
    },
  ) => api.post<Bug>(`/projects/${projectId}/bugs`, data),
  update: (
    projectId: string,
    id: string,
    data: Partial<{
      title: string;
      description: string;
      status: string;
      priority: string;
      severity: string;
      assigneeId: string;
      stepsToReproduce: string;
      expectedBehavior: string;
      actualBehavior: string;
      environment: string;
    }>,
  ) => api.put<Bug>(`/projects/${projectId}/bugs/${id}`, data),
  delete: (projectId: string, id: string) =>
    api.delete(`/projects/${projectId}/bugs/${id}`),
  getStats: (projectId: string) =>
    api.get<BugStats>(`/projects/${projectId}/bugs/stats`),
  search: (projectId: string, q: string) =>
    api.get<BugSummary[]>(`/projects/${projectId}/bugs/search`, {
      params: { q },
    }),
  getActivity: (projectId: string, id: string) =>
    api.get<ActivityLog[]>(`/projects/${projectId}/bugs/${id}/activity`),
  checkDuplicates: (projectId: string, id: string) =>
    api.get<DuplicateCheckResponse>(
      `/projects/${projectId}/bugs/${id}/duplicates`,
    ),
  uploadAttachment: (projectId: string, id: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return api.post<Attachment>(
      `/projects/${projectId}/bugs/${id}/attachments`,
      form,
      {
        headers: { "Content-Type": "multipart/form-data" },
      },
    );
  },
  deleteAttachment: (projectId: string, bugId: string, attachmentId: string) =>
    api.delete(
      `/projects/${projectId}/bugs/${bugId}/attachments/${attachmentId}`,
    ),
};

// Comments
export const commentApi = {
  getAll: (bugId: string) => api.get<Comment[]>(`/bugs/${bugId}/comments`),
  add: (bugId: string, content: string) =>
    api.post<Comment>(`/bugs/${bugId}/comments`, { content }),
  delete: (bugId: string, id: string) =>
    api.delete(`/bugs/${bugId}/comments/${id}`),
};

// Users
export const userApi = {
  getAll: () => api.get<User[]>("/users"),
  getById: (id: string) => api.get<User>(`/users/${id}`),
  updateRole: (id: string, role: string) =>
    api.patch<User>(`/users/${id}/role`, null, { params: { role } }),
};

export default api;
