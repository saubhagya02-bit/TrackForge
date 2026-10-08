// Enums
export type BugStatus =
  | "OPEN"
  | "IN_PROGRESS"
  | "IN_REVIEW"
  | "RESOLVED"
  | "CLOSED"
  | "REOPENED";
export type Priority = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
export type Severity = "BLOCKER" | "CRITICAL" | "MAJOR" | "MINOR" | "TRIVIAL";
export type UserRole = "ADMIN" | "PROJECT_MANAGER" | "DEVELOPER" | "TESTER";
export type ProjectStatus = "ACTIVE" | "ARCHIVED";

// Auth
export interface User {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  avatarUrl?: string;
  role: UserRole;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

// Projects
export interface Project {
  id: string;
  name: string;
  description?: string;
  key: string;
  status: ProjectStatus;
  owner: UserRef;
  totalBugs: number;
  openBugs: number;
  inProgressBugs: number;
  resolvedBugs: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectSummary {
  id: string;
  name: string;
  key: string;
}

// Bugs
export interface UserRef {
  id: string;
  username?: string;
  fullName?: string;
  avatarUrl?: string;
}

export interface Attachment {
  id: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  downloadUrl: string;
  createdAt: string;
}

export interface Bug {
  id: string;
  bugNumber: string;
  title: string;
  description?: string;
  status: BugStatus;
  priority: Priority;
  severity: Severity;
  project: ProjectSummary;
  reporter: UserRef;
  assignee?: UserRef;
  stepsToReproduce?: string;
  expectedBehavior?: string;
  actualBehavior?: string;
  environment?: string;
  commentCount: number;
  attachments: Attachment[];
  aiSuggestedPriority?: Priority;
  aiSuggestedSeverity?: Severity;
  aiConfidenceScore?: number;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
}

export interface BugSummary {
  id: string;
  bugNumber: string;
  title: string;
  status: BugStatus;
  priority: Priority;
  severity: Severity;
  assignee?: UserRef;
  createdAt: string;
}

export interface BugStats {
  total: number;
  byStatus: Record<string, number>;
  byPriority: Record<string, number>;
  resolvedThisWeek: number;
  avgResolutionHours: number;
}

export interface ActivityLog {
  id: string;
  actorUsername: string;
  type: string;
  fieldChanged?: string;
  oldValue?: string;
  newValue?: string;
  description?: string;
  createdAt: string;
}

export interface DuplicateCheckResponse {
  hasDuplicates: boolean;
  similarBugs: BugSummary[];
}

// Comments
export interface Comment {
  id: string;
  content: string;
  author: UserRef;
  edited: boolean;
  createdAt: string;
  updatedAt: string;
}

// Pagination
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// API params
export interface BugFilters {
  status?: BugStatus;
  priority?: Priority;
  assigneeId?: string;
  page?: number;
  size?: number;
  sortBy?: string;
}

// WebSocket
export interface WsMessage<T> {
  type: string;
  payload: T;
}
