import { useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ChevronLeft,
  Send,
  Trash2,
  Paperclip,
  Download,
  Sparkles,
} from "lucide-react";
import toast from "react-hot-toast";
import { bugApi, commentApi, userApi } from "@/services/api";
import { useAuthStore } from "@/store/authStore";
import { useBugUpdates } from "@/hooks/useWebSocket";
import {
  Spinner,
  Badge,
  FormField,
  Select,
  Textarea,
} from "@/components/ui";
import {
  statusColor,
  priorityColor,
  severityColor,
  formatDate,
  timeAgo,
  formatFileSize,
  STATUS_OPTIONS,
  PRIORITY_OPTIONS,
  SEVERITY_OPTIONS,
  avatarColor,
} from "@/utils/helpers";
import type { Comment } from "@/types";
import clsx from "clsx";

export default function BugDetailPage() {
  const { projectId, bugId } = useParams<{
    projectId: string;
    bugId: string;
  }>();
  const { user } = useAuthStore();
  const qc = useQueryClient();
  const [comment, setComment] = useState("");

  const { data: bug, isLoading } = useQuery({
    queryKey: ["bug", bugId],
    queryFn: () => bugApi.getById(projectId!, bugId!).then((r) => r.data),
    enabled: !!projectId && !!bugId,
  });

  const { data: comments = [] } = useQuery({
    queryKey: ["comments", bugId],
    queryFn: () => commentApi.getAll(bugId!).then((r) => r.data),
    enabled: !!bugId,
  });

  const { data: users = [] } = useQuery({
    queryKey: ["users"],
    queryFn: () => userApi.getAll().then((r) => r.data),
  });

  // Real-time bug updates via WebSocket
  useBugUpdates(bugId, () => {
    qc.invalidateQueries({ queryKey: ["bug", bugId] });
  });

  const updateMutation = useMutation({
    mutationFn: (data: Record<string, string>) =>
      bugApi.update(projectId!, bugId!, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["bug", bugId] });
      qc.invalidateQueries({ queryKey: ["bugs", projectId] });
      toast.success("Bug updated");
    },
    onError: () => toast.error("Failed to update bug"),
  });

  const commentMutation = useMutation({
    mutationFn: () => commentApi.add(bugId!, comment),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["comments", bugId] });
      setComment("");
      toast.success("Comment added");
    },
    onError: () => toast.error("Failed to add comment"),
  });

  const deleteCommentMutation = useMutation({
    mutationFn: (id: string) => commentApi.delete(bugId!, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["comments", bugId] }),
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) =>
      bugApi.uploadAttachment(projectId!, bugId!, file),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["bug", bugId] });
      toast.success("File uploaded");
    },
    onError: () => toast.error("Upload failed"),
  });

  if (isLoading)
    return (
      <div className="flex justify-center py-20">
        <Spinner className="text-indigo-500 w-8 h-8" />
      </div>
    );
  if (!bug) return null;

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) uploadMutation.mutate(file);
  };

  return (
    <div>
      <Link
        to={`/projects/${projectId}`}
        className="inline-flex items-center gap-1.5 text-sm text-slate-400 hover:text-slate-200 mb-5 transition-colors"
      >
        <ChevronLeft size={15} /> {bug.project?.name}
      </Link>

      {/* Bug header */}
      <div className="flex items-center gap-3 mb-2 flex-wrap">
        <span className="font-mono text-sm text-slate-500">
          {bug.bugNumber}
        </span>
        <Badge className={statusColor[bug.status]}>
          {bug.status.replace(/_/g, " ")}
        </Badge>
        <Badge className={priorityColor[bug.priority]}>{bug.priority}</Badge>
        <Badge className={severityColor[bug.severity]}>{bug.severity}</Badge>
      </div>
      <h1 className="text-2xl font-semibold text-slate-100 mb-6">
        {bug.title}
      </h1>

      {/* AI suggestion banner */}
      {bug.aiSuggestedPriority &&
        bug.aiConfidenceScore &&
        bug.aiConfidenceScore > 0 && (
          <div className="flex items-center gap-2 bg-indigo-600/10 border border-indigo-500/30 rounded-lg px-4 py-2.5 mb-5 text-sm">
            <Sparkles size={14} className="text-indigo-400" />
            <span className="text-indigo-300">
              AI suggests: <strong>{bug.aiSuggestedPriority}</strong> priority,{" "}
              <strong>{bug.aiSuggestedSeverity}</strong> severity
            </span>
            <span className="text-indigo-500 text-xs ml-auto">
              {Math.round((bug.aiConfidenceScore ?? 0) * 100)}% confidence
            </span>
          </div>
        )}

      <div className="grid lg:grid-cols-3 gap-6">
        {/* Main content */}
        <div className="lg:col-span-2 space-y-5">
          {bug.description && (
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
              <h3 className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-3">
                Description
              </h3>
              <p className="text-sm text-slate-300 whitespace-pre-wrap leading-relaxed">
                {bug.description}
              </p>
            </div>
          )}

          {bug.stepsToReproduce && (
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
              <h3 className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-3">
                Steps to reproduce
              </h3>
              <p className="text-sm text-slate-300 whitespace-pre-wrap font-mono leading-relaxed">
                {bug.stepsToReproduce}
              </p>
            </div>
          )}

          {(bug.expectedBehavior || bug.actualBehavior) && (
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 grid sm:grid-cols-2 gap-4">
              {bug.expectedBehavior && (
                <div>
                  <h3 className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-2">
                    Expected
                  </h3>
                  <p className="text-sm text-slate-300">
                    {bug.expectedBehavior}
                  </p>
                </div>
              )}
              {bug.actualBehavior && (
                <div>
                  <h3 className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-2">
                    Actual
                  </h3>
                  <p className="text-sm text-slate-300">{bug.actualBehavior}</p>
                </div>
              )}
            </div>
          )}

          {/* Attachments */}
          {(bug.attachments?.length ?? 0) > 0 && (
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
              <h3 className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-3">
                Attachments ({bug.attachments.length})
              </h3>
              <div className="space-y-2">
                {bug.attachments.map((a) => (
                  <div
                    key={a.id}
                    className="flex items-center gap-3 p-2 rounded-lg hover:bg-slate-800 transition-colors"
                  >
                    <Paperclip
                      size={13}
                      className="text-slate-500 flex-shrink-0"
                    />
                    <span className="text-sm text-slate-300 flex-1 truncate">
                      {a.originalFilename}
                    </span>
                    <span className="text-xs text-slate-500">
                      {formatFileSize(a.fileSize)}
                    </span>
                    {a.downloadUrl && (
                      <a
                        href={a.downloadUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="p-1 rounded text-slate-500 hover:text-indigo-400 transition-colors"
                      >
                        <Download size={13} />
                      </a>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Upload attachment */}
          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 px-3 py-2 rounded-lg border border-slate-700 text-sm text-slate-400 hover:text-slate-200 hover:bg-slate-800 cursor-pointer transition-colors">
              <Paperclip size={14} />
              {uploadMutation.isPending ? "Uploading..." : "Attach file"}
              <input
                type="file"
                className="hidden"
                onChange={handleFile}
                disabled={uploadMutation.isPending}
              />
            </label>
          </div>

          {/* Comments */}
          <div>
            <h3 className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-3">
              Comments ({comments.length})
            </h3>
            <div className="space-y-3 mb-4">
              {comments.map((c: Comment) => (
                <div
                  key={c.id}
                  className="bg-slate-900 border border-slate-800 rounded-xl p-4"
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <div
                        className={clsx(
                          "w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white",
                          avatarColor(c.author?.id ?? ""),
                        )}
                      >
                        {c.author?.username?.[0]?.toUpperCase()}
                      </div>
                      <span className="text-sm font-medium text-slate-300">
                        {c.author?.username}
                      </span>
                      <span className="text-xs text-slate-500">
                        {timeAgo(c.createdAt)}
                      </span>
                      {c.edited && (
                        <span className="text-xs text-slate-600">(edited)</span>
                      )}
                    </div>
                    {c.author?.username === user?.username && (
                      <button
                        onClick={() => deleteCommentMutation.mutate(c.id)}
                        className="p-1 rounded text-slate-600 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                      >
                        <Trash2 size={12} />
                      </button>
                    )}
                  </div>
                  <p className="text-sm text-slate-300 whitespace-pre-wrap leading-relaxed">
                    {c.content}
                  </p>
                </div>
              ))}
            </div>

            <div className="flex gap-3">
              <Textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                rows={2}
                placeholder="Leave a comment..."
                className="flex-1"
                onKeyDown={(e) => {
                  if (e.key === "Enter" && e.ctrlKey && comment.trim()) {
                    commentMutation.mutate();
                  }
                }}
              />
              <button
                onClick={() => comment.trim() && commentMutation.mutate()}
                disabled={!comment.trim() || commentMutation.isPending}
                className="self-end bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white p-2.5 rounded-lg transition-colors"
              >
                {commentMutation.isPending ? (
                  <Spinner className="w-4 h-4" />
                ) : (
                  <Send size={15} />
                )}
              </button>
            </div>
            <p className="text-xs text-slate-600 mt-1">Ctrl+Enter to submit</p>
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-4">
          {/* Editable fields */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-4">
            <FormField label="Status">
              <Select
                value={bug.status}
                onChange={(e) =>
                  updateMutation.mutate({ status: e.target.value })
                }
              >
                {STATUS_OPTIONS.map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </Select>
            </FormField>
            <FormField label="Priority">
              <Select
                value={bug.priority}
                onChange={(e) =>
                  updateMutation.mutate({ priority: e.target.value })
                }
              >
                {PRIORITY_OPTIONS.map((p) => (
                  <option key={p}>{p}</option>
                ))}
              </Select>
            </FormField>
            <FormField label="Severity">
              <Select
                value={bug.severity}
                onChange={(e) =>
                  updateMutation.mutate({ severity: e.target.value })
                }
              >
                {SEVERITY_OPTIONS.map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </Select>
            </FormField>
            <FormField label="Assignee">
              <Select
                value={bug.assignee?.id ?? ""}
                onChange={(e) =>
                  updateMutation.mutate({ assigneeId: e.target.value })
                }
              >
                <option value="">Unassigned</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.fullName || u.username}
                  </option>
                ))}
              </Select>
            </FormField>
          </div>

          {/* Meta info */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-3 text-sm">
            <div>
              <p className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-1">
                Reporter
              </p>
              <p className="text-slate-300">{bug.reporter?.username ?? "—"}</p>
            </div>
            {bug.environment && (
              <div>
                <p className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-1">
                  Environment
                </p>
                <p className="text-slate-300 font-mono text-xs">
                  {bug.environment}
                </p>
              </div>
            )}
            <div>
              <p className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-1">
                Created
              </p>
              <p className="text-slate-300">{formatDate(bug.createdAt)}</p>
            </div>
            <div>
              <p className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-1">
                Updated
              </p>
              <p className="text-slate-300">{timeAgo(bug.updatedAt)}</p>
            </div>
            {bug.resolvedAt && (
              <div>
                <p className="text-xs font-medium text-slate-400 uppercase tracking-wide mb-1">
                  Resolved
                </p>
                <p className="text-slate-300">{formatDate(bug.resolvedAt)}</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
