import type { BugStatus, Priority, Severity, UserRole } from "@/types";

export const statusColor: Record<BugStatus, string> = {
  OPEN: "bg-blue-500/20 text-blue-400 border border-blue-500/30",
  IN_PROGRESS: "bg-purple-500/20 text-purple-400 border border-purple-500/30",
  IN_REVIEW: "bg-indigo-500/20 text-indigo-400 border border-indigo-500/30",
  RESOLVED: "bg-green-500/20 text-green-400 border border-green-500/30",
  CLOSED: "bg-slate-500/20 text-slate-400 border border-slate-500/30",
  REOPENED: "bg-red-500/20 text-red-400 border border-red-500/30",
};

export const priorityColor: Record<Priority, string> = {
  CRITICAL: "bg-red-500/20 text-red-400 border border-red-500/30",
  HIGH: "bg-orange-500/20 text-orange-400 border border-orange-500/30",
  MEDIUM: "bg-yellow-500/20 text-yellow-400 border border-yellow-500/30",
  LOW: "bg-slate-500/20 text-slate-400 border border-slate-500/30",
};

export const severityColor: Record<Severity, string> = {
  BLOCKER: "bg-red-600/20 text-red-300",
  CRITICAL: "bg-red-500/20 text-red-400",
  MAJOR: "bg-orange-500/20 text-orange-400",
  MINOR: "bg-yellow-500/20 text-yellow-400",
  TRIVIAL: "bg-slate-500/20 text-slate-400",
};

export const roleColor: Record<UserRole, string> = {
  ADMIN: "bg-red-500/20 text-red-400",
  PROJECT_MANAGER: "bg-purple-500/20 text-purple-400",
  DEVELOPER: "bg-blue-500/20 text-blue-400",
  TESTER: "bg-green-500/20 text-green-400",
};

export const STATUS_OPTIONS: BugStatus[] = [
  "OPEN",
  "IN_PROGRESS",
  "IN_REVIEW",
  "RESOLVED",
  "CLOSED",
  "REOPENED",
];
export const PRIORITY_OPTIONS: Priority[] = [
  "CRITICAL",
  "HIGH",
  "MEDIUM",
  "LOW",
];
export const SEVERITY_OPTIONS: Severity[] = [
  "BLOCKER",
  "CRITICAL",
  "MAJOR",
  "MINOR",
  "TRIVIAL",
];

export function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return "just now";
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.floor(h / 24);
  if (d < 30) return `${d}d ago`;
  return formatDate(dateStr);
}

export function formatDate(dateStr: string): string {
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(new Date(dateStr));
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function getInitials(name?: string, fallback = "?"): string {
  if (!name) return fallback;
  return name
    .split(" ")
    .map((w) => w[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
}

export function avatarColor(username: string): string {
  const colors = [
    "bg-indigo-600",
    "bg-purple-600",
    "bg-blue-600",
    "bg-green-600",
    "bg-rose-600",
    "bg-orange-600",
  ];
  let hash = 0;
  for (const c of username) hash = (hash << 5) - hash + c.charCodeAt(0);
  return colors[Math.abs(hash) % colors.length];
}
