import { useState, useCallback } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Plus,
  ChevronLeft,
  LayoutList,
  Columns,
  Search,
  X,
} from "lucide-react";
import toast from "react-hot-toast";
import { projectApi, bugApi, userApi } from "@/services/api";
import { useProjectBugUpdates } from "@/hooks/useWebSocket";
import KanbanBoard from "@/components/kanban/KanbanBoard";
import {
  Spinner,
  Badge,
  Modal,
  FormField,
  Button,
  EmptyState,
  PageHeader,
  Input,
  Select,
  Textarea,
  ErrorBanner,
} from "@/components/ui";
import {
  statusColor,
  priorityColor,
  timeAgo,
  STATUS_OPTIONS,
  PRIORITY_OPTIONS,
  SEVERITY_OPTIONS,
} from "@/utils/helpers";
import { avatarColor } from "@/utils/helpers";
import type { BugSummary, BugStatus, Priority } from "@/types";
import clsx from "clsx";

function CreateBugModal({
  open,
  onClose,
  projectId,
}: {
  open: boolean;
  onClose: () => void;
  projectId: string;
}) {
  const qc = useQueryClient();
  const { data: users = [] } = useQuery({
    queryKey: ["users"],
    queryFn: () => userApi.getAll().then((r) => r.data),
  });
  const [form, setForm] = useState({
    title: "",
    description: "",
    priority: "MEDIUM",
    severity: "MINOR",
    assigneeId: "",
    stepsToReproduce: "",
    expectedBehavior: "",
    actualBehavior: "",
    environment: "",
  });
  const [error, setError] = useState("");
  const handle = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const mutation = useMutation({
    mutationFn: () =>
      bugApi.create(projectId, {
        ...form,
        assigneeId: form.assigneeId || undefined,
      }),
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: ["bugs", projectId] });
      qc.invalidateQueries({ queryKey: ["projects"] });
      toast.success(`Bug ${res.data.bugNumber} created`);
      onClose();
      setForm({
        title: "",
        description: "",
        priority: "MEDIUM",
        severity: "MINOR",
        assigneeId: "",
        stepsToReproduce: "",
        expectedBehavior: "",
        actualBehavior: "",
        environment: "",
      });
    },
    onError: (err: any) =>
      setError(err.response?.data?.message || "Failed to create bug"),
  });

  return (
    <Modal open={open} onClose={onClose} title="Report a bug" size="lg">
      <ErrorBanner message={error} />
      <div className="space-y-4">
        <FormField label="Title *">
          <Input
            name="title"
            value={form.title}
            onChange={handle}
            placeholder="Short, clear description"
            required
          />
        </FormField>
        <FormField label="Description">
          <Textarea
            name="description"
            value={form.description}
            onChange={handle}
            rows={3}
            placeholder="More details about the bug..."
          />
        </FormField>
        <div className="grid grid-cols-2 gap-3">
          <FormField label="Priority">
            <Select name="priority" value={form.priority} onChange={handle}>
              {PRIORITY_OPTIONS.map((o) => (
                <option key={o}>{o}</option>
              ))}
            </Select>
          </FormField>
          <FormField label="Severity">
            <Select name="severity" value={form.severity} onChange={handle}>
              {SEVERITY_OPTIONS.map((o) => (
                <option key={o}>{o}</option>
              ))}
            </Select>
          </FormField>
        </div>
        <FormField label="Assign to">
          <Select name="assigneeId" value={form.assigneeId} onChange={handle}>
            <option value="">Unassigned</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.fullName || u.username}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Steps to reproduce">
          <Textarea
            name="stepsToReproduce"
            value={form.stepsToReproduce}
            onChange={handle}
            rows={3}
            placeholder="1. Go to...&#10;2. Click on...&#10;3. See error"
          />
        </FormField>
        <div className="grid grid-cols-2 gap-3">
          <FormField label="Expected">
            <Textarea
              name="expectedBehavior"
              value={form.expectedBehavior}
              onChange={handle}
              rows={2}
            />
          </FormField>
          <FormField label="Actual">
            <Textarea
              name="actualBehavior"
              value={form.actualBehavior}
              onChange={handle}
              rows={2}
            />
          </FormField>
        </div>
        <FormField label="Environment">
          <Input
            name="environment"
            value={form.environment}
            onChange={handle}
            placeholder="Chrome 126, macOS 14, Production"
          />
        </FormField>
        <div className="flex gap-3 pt-1">
          <Button variant="ghost" onClick={onClose} className="flex-1">
            Cancel
          </Button>
          <Button
            onClick={() => mutation.mutate()}
            loading={mutation.isPending}
            className="flex-1"
          >
            Report bug
          </Button>
        </div>
      </div>
    </Modal>
  );
}

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [view, setView] = useState<"list" | "kanban">("list");
  const [filters, setFilters] = useState<{
    status: string;
    priority: string;
    search: string;
    page: number;
  }>({ status: "", priority: "", search: "", page: 0 });

  const { data: project, isLoading: projectLoading } = useQuery({
    queryKey: ["project", id],
    queryFn: () => projectApi.getById(id!).then((r) => r.data),
    enabled: !!id,
  });

  const { data: bugsPage, isLoading: bugsLoading } = useQuery({
    queryKey: ["bugs", id, filters],
    queryFn: () =>
      bugApi
        .getAll(id!, {
          status: (filters.status as BugStatus) || undefined,
          priority: (filters.priority as Priority) || undefined,
          page: filters.page,
          size: 20,
        })
        .then((r) => r.data),
    enabled: !!id,
  });

  // Real-time updates via WebSocket
  const handleWsUpdate = useCallback(
    (data: unknown) => {
      qc.invalidateQueries({ queryKey: ["bugs", id] });
      toast.success("Bug list updated in real-time");
      console.log("[WS] Project bug update:", data);
    },
    [id, qc],
  );
  useProjectBugUpdates(id, handleWsUpdate);

  const { data: searchResults } = useQuery({
    queryKey: ["bug-search", id, filters.search],
    queryFn: () => bugApi.search(id!, filters.search).then((r) => r.data),
    enabled: !!id && filters.search.length > 2,
  });

  const bugs: BugSummary[] =
    filters.search.length > 2
      ? (searchResults ?? [])
      : (bugsPage?.content ?? []);

  const handleBugClick = (bug: BugSummary) =>
    navigate(`/projects/${id}/bugs/${bug.id}`);
  const setFilter = (key: string, value: string) =>
    setFilters((f) => ({ ...f, [key]: value, page: 0 }));

  if (projectLoading)
    return (
      <div className="flex justify-center py-20">
        <Spinner className="text-indigo-500 w-8 h-8" />
      </div>
    );
  if (!project) return null;

  return (
    <div>
      <Link
        to="/projects"
        className="inline-flex items-center gap-1.5 text-sm text-slate-400 hover:text-slate-200 mb-5 transition-colors"
      >
        <ChevronLeft size={15} /> Projects
      </Link>

      <PageHeader
        title={project.name}
        subtitle={
          <span className="flex items-center gap-2">
            <span className="font-mono text-xs text-indigo-400 bg-indigo-600/10 px-2 py-0.5 rounded">
              {project.key}
            </span>
            <span>{project.description}</span>
          </span>
        }
        action={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus size={15} /> Report bug
          </Button>
        }
      />

      {/* Stats row */}
      <div className="grid grid-cols-4 gap-3 mb-5">
        {(
          [
            ["Total", project.totalBugs, "text-slate-400"],
            ["Open", project.openBugs, "text-blue-400"],
            ["In Progress", project.inProgressBugs, "text-purple-400"],
            ["Resolved", project.resolvedBugs, "text-green-400"],
          ] as const
        ).map(([label, val, color]) => (
          <div
            key={label}
            className="bg-slate-900 border border-slate-800 rounded-lg p-3"
          >
            <p className="text-xs text-slate-500 mb-1">{label}</p>
            <p className={clsx("text-xl font-semibold", color)}>{val}</p>
          </div>
        ))}
      </div>

      {/* Toolbar */}
      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div className="relative flex-1 min-w-[200px]">
          <Search
            size={14}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500"
          />
          <Input
            value={filters.search}
            onChange={(e) => setFilter("search", e.target.value)}
            placeholder="Search bugs..."
            className="pl-8 py-1.5"
          />
          {filters.search && (
            <button
              onClick={() => setFilter("search", "")}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
            >
              <X size={13} />
            </button>
          )}
        </div>
        <select
          value={filters.status}
          onChange={(e) => setFilter("status", e.target.value)}
          className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-sm text-slate-300 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        >
          <option value="">All statuses</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
        <select
          value={filters.priority}
          onChange={(e) => setFilter("priority", e.target.value)}
          className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-sm text-slate-300 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        >
          <option value="">All priorities</option>
          {PRIORITY_OPTIONS.map((p) => (
            <option key={p}>{p}</option>
          ))}
        </select>
        <div className="flex items-center bg-slate-800 border border-slate-700 rounded-lg p-0.5">
          <button
            onClick={() => setView("list")}
            className={clsx(
              "p-1.5 rounded",
              view === "list"
                ? "bg-slate-700 text-slate-100"
                : "text-slate-500 hover:text-slate-300",
            )}
          >
            <LayoutList size={15} />
          </button>
          <button
            onClick={() => setView("kanban")}
            className={clsx(
              "p-1.5 rounded",
              view === "kanban"
                ? "bg-slate-700 text-slate-100"
                : "text-slate-500 hover:text-slate-300",
            )}
          >
            <Columns size={15} />
          </button>
        </div>
      </div>

      {/* Bug view */}
      {bugsLoading ? (
        <div className="flex justify-center py-12">
          <Spinner className="text-indigo-500 w-6 h-6" />
        </div>
      ) : view === "kanban" ? (
        <KanbanBoard projectId={id!} bugs={bugs} onBugClick={handleBugClick} />
      ) : bugs.length === 0 ? (
        <EmptyState
          title="No bugs found"
          description="No bugs match the current filters."
        />
      ) : (
        <>
          <div className="bg-slate-900 border border-slate-800 rounded-xl divide-y divide-slate-800">
            {bugs.map((bug) => (
              <button
                key={bug.id}
                onClick={() => handleBugClick(bug)}
                className="w-full flex items-center gap-4 px-5 py-4 hover:bg-slate-800/50 transition-colors group text-left"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1 flex-wrap">
                    <span className="font-mono text-xs text-slate-500">
                      {bug.bugNumber}
                    </span>
                    <Badge className={statusColor[bug.status]}>
                      {bug.status.replace(/_/g, " ")}
                    </Badge>
                    <Badge className={priorityColor[bug.priority]}>
                      {bug.priority}
                    </Badge>
                  </div>
                  <p className="text-sm font-medium text-slate-200 group-hover:text-indigo-300 transition-colors truncate">
                    {bug.title}
                  </p>
                </div>
                <div className="flex items-center gap-3 flex-shrink-0 text-xs text-slate-500">
                  {bug.assignee?.id && (
                    <div
                      className={clsx(
                        "w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium text-white",
                        avatarColor(bug.assignee.id),
                      )}
                      title={bug.assignee.username}
                    >
                      {bug.assignee.username?.[0]?.toUpperCase()}
                    </div>
                  )}
                  <span>{timeAgo(bug.createdAt)}</span>
                </div>
              </button>
            ))}
          </div>
          {bugsPage && bugsPage.totalPages > 1 && (
            <div className="flex justify-center gap-3 mt-5">
              <Button
                variant="ghost"
                disabled={filters.page === 0}
                onClick={() => setFilters((f) => ({ ...f, page: f.page - 1 }))}
              >
                Previous
              </Button>
              <span className="text-sm text-slate-400 self-center">
                Page {filters.page + 1} of {bugsPage.totalPages}
              </span>
              <Button
                variant="ghost"
                disabled={filters.page >= bugsPage.totalPages - 1}
                onClick={() => setFilters((f) => ({ ...f, page: f.page + 1 }))}
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}

      <CreateBugModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        projectId={id!}
      />
    </div>
  );
}
