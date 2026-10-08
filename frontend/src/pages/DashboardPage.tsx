import { useQuery } from "@tanstack/react-query";
import {
  FolderKanban,
  Bug,
  AlertCircle,
  CheckCircle,
  Clock,
  type LucideIcon
} from "lucide-react";
import { Link } from "react-router-dom";
import { projectApi } from "@/services/api";
import { useAuthStore } from "@/store/authStore";
import { Spinner, Badge, PageHeader } from "@/components/ui";
import { timeAgo } from "@/utils/helpers";
import type { Project } from "@/types";

function StatCard({
  icon: Icon,
  label,
  value,
  color,
}: {
  icon: LucideIcon;
  label: string;
  value: number;
  color: string;
}) {
  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
      <div className="flex items-center justify-between mb-3">
        <span className="text-slate-400 text-sm">{label}</span>
        <div
          className={`w-8 h-8 rounded-lg flex items-center justify-center ${color}`}
        >
          <Icon size={15} />
        </div>
      </div>
      <p className="text-3xl font-semibold text-slate-100">{value}</p>
    </div>
  );
}

export default function DashboardPage() {
  const { user } = useAuthStore();

  const { data: projects = [], isLoading } = useQuery({
    queryKey: ["projects"],
    queryFn: () => projectApi.getAll().then((r) => r.data),
  });

  const totalBugs = projects.reduce((s, p) => s + p.totalBugs, 0);
  const openBugs = projects.reduce((s, p) => s + p.openBugs, 0);
  const resolvedBugs = projects.reduce((s, p) => s + p.resolvedBugs, 0);

  if (isLoading)
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner className="text-indigo-500 w-8 h-8" />
      </div>
    );

  return (
    <div>
      <PageHeader
        title={`Good to see you, ${user?.fullName || user?.username} 👋`}
        subtitle="Here's what's happening across your projects."
      />

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard
          icon={FolderKanban}
          label="Projects"
          value={projects.length}
          color="bg-indigo-600/20 text-indigo-400"
        />
        <StatCard
          icon={Bug}
          label="Total bugs"
          value={totalBugs}
          color="bg-slate-700/50 text-slate-400"
        />
        <StatCard
          icon={AlertCircle}
          label="Open"
          value={openBugs}
          color="bg-blue-500/20 text-blue-400"
        />
        <StatCard
          icon={CheckCircle}
          label="Resolved"
          value={resolvedBugs}
          color="bg-green-500/20 text-green-400"
        />
      </div>

      <h2 className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-3">
        Your projects
      </h2>

      {projects.length === 0 ? (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-10 text-center">
          <FolderKanban size={36} className="text-slate-600 mx-auto mb-3" />
          <p className="text-slate-400">
            No projects yet.{" "}
            <Link to="/projects" className="text-indigo-400 hover:underline">
              Create one
            </Link>
          </p>
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((p: Project) => (
            <Link
              key={p.id}
              to={`/projects/${p.id}`}
              className="bg-slate-900 border border-slate-800 rounded-xl p-5 hover:border-slate-700 transition-colors group"
            >
              <div className="flex items-start justify-between mb-3">
                <span className="font-mono text-xs text-indigo-400 bg-indigo-600/10 px-2 py-0.5 rounded font-medium">
                  {p.key}
                </span>
                <Badge
                  className={
                    p.status === "ACTIVE"
                      ? "bg-green-500/20 text-green-400"
                      : "bg-slate-500/20 text-slate-400"
                  }
                >
                  {p.status}
                </Badge>
              </div>
              <h3 className="font-medium text-slate-100 group-hover:text-indigo-300 transition-colors mb-1">
                {p.name}
              </h3>
              {p.description && (
                <p className="text-slate-500 text-sm line-clamp-2 mb-3">
                  {p.description}
                </p>
              )}
              <div className="flex items-center gap-4 text-xs text-slate-500">
                <span className="flex items-center gap-1">
                  <Bug size={11} /> {p.totalBugs} bugs
                </span>
                <span className="flex items-center gap-1">
                  <AlertCircle size={11} /> {p.openBugs} open
                </span>
                <span className="flex items-center gap-1">
                  <Clock size={11} /> {timeAgo(p.createdAt)}
                </span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
