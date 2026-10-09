import { useQuery } from "@tanstack/react-query";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import { projectApi, bugApi } from "@/services/api";
import { PageHeader, Spinner } from "@/components/ui";

const COLORS = [
  "#3b82f6",
  "#a855f7",
  "#6366f1",
  "#22c55e",
  "#64748b",
  "#ef4444",
];
const STATUS_COLORS: Record<string, string> = {
  OPEN: "#3b82f6",
  IN_PROGRESS: "#a855f7",
  IN_REVIEW: "#6366f1",
  RESOLVED: "#22c55e",
  CLOSED: "#64748b",
  REOPENED: "#ef4444",
};

function StatCard({
  label,
  value,
  sub,
}: {
  label: string;
  value: number | string;
  sub?: string;
}) {
  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
      <p className="text-xs text-slate-500 mb-2 uppercase tracking-wide">
        {label}
      </p>
      <p className="text-3xl font-semibold text-slate-100">{value}</p>
      {sub && <p className="text-xs text-slate-500 mt-1">{sub}</p>}
    </div>
  );
}

export default function AnalyticsPage() {
  const { data: projects = [], isLoading } = useQuery({
    queryKey: ["projects"],
    queryFn: () => projectApi.getAll().then((r) => r.data),
  });

  const { data: firstStats } = useQuery({
    queryKey: ["bugStats", projects[0]?.id],
    queryFn: () => bugApi.getStats(projects[0].id).then((r) => r.data),
    enabled: projects.length > 0,
  });

  if (isLoading)
    return (
      <div className="flex justify-center py-20">
        <Spinner className="text-indigo-500 w-8 h-8" />
      </div>
    );

  const totalBugs = projects.reduce((s, p) => s + p.totalBugs, 0);
  const openBugs = projects.reduce((s, p) => s + p.openBugs, 0);
  const resolvedBugs = projects.reduce((s, p) => s + p.resolvedBugs, 0);
  const resolutionRate =
    totalBugs > 0 ? Math.round((resolvedBugs / totalBugs) * 100) : 0;

  const projectData = projects.map((p) => ({
    name: p.key,
    open: p.openBugs,
    resolved: p.resolvedBugs,
    total: p.totalBugs,
  }));

  const statusData = firstStats
    ? Object.entries(firstStats.byStatus).map(([status, count]) => ({
        name: status.replace(/_/g, " "),
        value: count,
        status,
      }))
    : [];

  const priorityData = firstStats
    ? Object.entries(firstStats.byPriority).map(([priority, count]) => ({
        name: priority,
        value: count,
      }))
    : [];

  const tooltipStyle = {
    contentStyle: {
      background: "#1e293b",
      border: "1px solid #334155",
      borderRadius: "8px",
      color: "#f1f5f9",
      fontSize: "12px",
    },
    itemStyle: { color: "#94a3b8" },
  };

  return (
    <div>
      <PageHeader
        title="Analytics"
        subtitle="Project and bug metrics overview"
      />

      {/* Summary stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard label="Total bugs" value={totalBugs} />
        <StatCard label="Open" value={openBugs} sub="Need attention" />
        <StatCard label="Resolved" value={resolvedBugs} />
        <StatCard
          label="Resolution rate"
          value={`${resolutionRate}%`}
          sub="of all bugs"
        />
      </div>

      {/* Charts row 1 */}
      <div className="grid lg:grid-cols-2 gap-6 mb-6">
        {/* Bugs per project */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <h3 className="text-sm font-medium text-slate-300 mb-4">
            Bugs per project
          </h3>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={projectData} barSize={20}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
              <XAxis
                dataKey="name"
                tick={{ fill: "#64748b", fontSize: 12 }}
                axisLine={false}
              />
              <YAxis
                tick={{ fill: "#64748b", fontSize: 12 }}
                axisLine={false}
              />
              <Tooltip {...tooltipStyle} />
              <Bar
                dataKey="open"
                name="Open"
                fill="#3b82f6"
                radius={[4, 4, 0, 0]}
              />
              <Bar
                dataKey="resolved"
                name="Resolved"
                fill="#22c55e"
                radius={[4, 4, 0, 0]}
              />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Bug status distribution */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <h3 className="text-sm font-medium text-slate-300 mb-4">
            Status distribution {projects[0] ? `— ${projects[0].name}` : ""}
          </h3>
          {statusData.length > 0 ? (
            <div className="flex items-center gap-4">
              <ResponsiveContainer width="60%" height={200}>
                <PieChart>
                  <Pie
                    data={statusData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={85}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {statusData.map((entry) => (
                      <Cell
                        key={entry.name}
                        fill={STATUS_COLORS[entry.status] ?? "#64748b"}
                      />
                    ))}
                  </Pie>
                  <Tooltip {...tooltipStyle} />
                </PieChart>
              </ResponsiveContainer>
              <div className="flex flex-col gap-2">
                {statusData.map((entry) => (
                  <div
                    key={entry.name}
                    className="flex items-center gap-2 text-xs"
                  >
                    <div
                      className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                      style={{
                        background: STATUS_COLORS[entry.status] ?? "#64748b",
                      }}
                    />
                    <span className="text-slate-400">{entry.name}</span>
                    <span className="text-slate-200 font-medium ml-auto pl-2">
                      {entry.value}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="flex items-center justify-center h-48 text-slate-600 text-sm">
              No data yet
            </div>
          )}
        </div>
      </div>

      {/* Priority breakdown */}
      {priorityData.length > 0 && (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 mb-6">
          <h3 className="text-sm font-medium text-slate-300 mb-4">
            Priority breakdown {projects[0] ? `— ${projects[0].name}` : ""}
          </h3>
          <ResponsiveContainer width="100%" height={180}>
            <BarChart data={priorityData} layout="vertical" barSize={16}>
              <CartesianGrid
                strokeDasharray="3 3"
                stroke="#1e293b"
                horizontal={false}
              />
              <XAxis
                type="number"
                tick={{ fill: "#64748b", fontSize: 12 }}
                axisLine={false}
              />
              <YAxis
                type="category"
                dataKey="name"
                tick={{ fill: "#64748b", fontSize: 12 }}
                axisLine={false}
                width={70}
              />
              <Tooltip {...tooltipStyle} />
              <Bar dataKey="value" name="Bugs" radius={[0, 4, 4, 0]}>
                {priorityData.map((entry, i) => (
                  <Cell key={entry.name} fill={COLORS[i % COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Project table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-800">
          <h3 className="text-sm font-medium text-slate-300">
            All projects summary
          </h3>
        </div>
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-800">
              {[
                "Project",
                "Key",
                "Total",
                "Open",
                "In Progress",
                "Resolved",
                "Status",
              ].map((h) => (
                <th
                  key={h}
                  className="px-5 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wide"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {projects.map((p) => (
              <tr
                key={p.id}
                className="hover:bg-slate-800/50 transition-colors"
              >
                <td className="px-5 py-3 text-sm font-medium text-slate-200">
                  {p.name}
                </td>
                <td className="px-5 py-3">
                  <span className="font-mono text-xs text-indigo-400 bg-indigo-600/10 px-2 py-0.5 rounded">
                    {p.key}
                  </span>
                </td>
                <td className="px-5 py-3 text-sm text-slate-300">
                  {p.totalBugs}
                </td>
                <td className="px-5 py-3 text-sm text-blue-400">
                  {p.openBugs}
                </td>
                <td className="px-5 py-3 text-sm text-purple-400">
                  {p.inProgressBugs}
                </td>
                <td className="px-5 py-3 text-sm text-green-400">
                  {p.resolvedBugs}
                </td>
                <td className="px-5 py-3">
                  <span
                    className={`text-xs px-2 py-0.5 rounded font-medium ${p.status === "ACTIVE" ? "bg-green-500/20 text-green-400" : "bg-slate-500/20 text-slate-400"}`}
                  >
                    {p.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
