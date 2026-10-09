import { useState } from "react";
import { Link } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, FolderKanban, Bug, AlertCircle, Trash2 } from "lucide-react";
import toast from "react-hot-toast";

import { projectApi } from "@/services/api";

import {
  Spinner,
  Badge,
  Modal,
  FormField,
  Input,
  Textarea,
  Button,
  PageHeader,
  EmptyState,
  ErrorBanner,
} from "@/components/ui";

import { timeAgo } from "@/utils/helpers";

function CreateProjectModal({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const qc = useQueryClient();

  const [form, setForm] = useState({
    name: "",
    key: "",
    description: "",
  });

  const [error, setError] = useState("");

  const handle = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    setForm((f) => ({
      ...f,
      [e.target.name]: e.target.value,
    }));
  };

  const mutation = useMutation({
    mutationFn: () => projectApi.create(form),

    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: ["projects"] });

      toast.success(`Project ${res.data.key} created`);

      onClose();

      setForm({
        name: "",
        key: "",
        description: "",
      });

      setError("");
    },

    onError: (err: any) => {
      setError(err.response?.data?.message || "Failed to create project");
    },
  });

  return (
    <Modal open={open} onClose={onClose} title="New project">
      {error && <ErrorBanner message={error} />}

      <div className="space-y-4">
        <FormField label="Project name">
          <Input
            name="name"
            value={form.name}
            onChange={handle}
            placeholder="My App"
            required
          />
        </FormField>

        <FormField
          label="Project key"
          hint="2–20 uppercase characters e.g. APP, MYAPP"
        >
          <Input
            name="key"
            value={form.key}
            onChange={(e) =>
              setForm((f) => ({
                ...f,
                key: e.target.value.toUpperCase(),
              }))
            }
            placeholder="APP"
            maxLength={20}
            className="font-mono"
            required
          />
        </FormField>

        <FormField label="Description">
          <Textarea
            name="description"
            value={form.description}
            onChange={handle}
            rows={3}
            placeholder="What is this project about?"
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
            Create project
          </Button>
        </div>
      </div>
    </Modal>
  );
}

export default function ProjectsPage() {
  const [createOpen, setCreateOpen] = useState(false);

  const qc = useQueryClient();

  const { data: projects = [], isLoading } = useQuery({
    queryKey: ["projects"],
    queryFn: () => projectApi.getAll().then((r) => r.data),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => projectApi.delete(id),

    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["projects"] });
      toast.success("Project deleted");
    },

    onError: () => {
      toast.error("Failed to delete project");
    },
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner className="text-indigo-500 w-8 h-8" />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Projects"
        subtitle={`${projects.length} project${
          projects.length !== 1 ? "s" : ""
        }`}
        action={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus size={15} />
            New project
          </Button>
        }
      />

      {projects.length === 0 ? (
        <EmptyState
          icon={FolderKanban}
          title="No projects yet"
          description="Create your first project to start tracking bugs."
          action={
            <Button onClick={() => setCreateOpen(true)}>
              <Plus size={15} />
              Create project
            </Button>
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((p) => (
            <div
              key={p.id}
              className="bg-slate-900 border border-slate-800 rounded-xl p-5 hover:border-slate-700 transition-colors group"
            >
              <div className="flex items-start justify-between mb-3">
                <span className="font-mono text-xs text-indigo-400 bg-indigo-600/10 px-2 py-0.5 rounded font-medium">
                  {p.key}
                </span>

                <div className="flex items-center gap-1.5">
                  <Badge
                    className={
                      p.status === "ACTIVE"
                        ? "bg-green-500/20 text-green-400"
                        : "bg-slate-500/20 text-slate-400"
                    }
                  >
                    {p.status}
                  </Badge>

                  <button
                    type="button"
                    onClick={() => {
                      if (confirm("Delete this project and all its bugs?")) {
                        deleteMutation.mutate(p.id);
                      }
                    }}
                    className="p-1.5 rounded text-slate-600 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                    title="Delete project"
                  >
                    <Trash2 size={13} />
                  </button>
                </div>
              </div>

              <Link to={`/projects/${p.id}`} className="block">
                <h3 className="font-medium text-slate-100 group-hover:text-indigo-300 transition-colors mb-1">
                  {p.name}
                </h3>

                {p.description && (
                  <p className="text-slate-500 text-sm line-clamp-2 mb-3">
                    {p.description}
                  </p>
                )}
              </Link>

              <div className="flex items-center gap-4 text-xs text-slate-500 mt-2">
                <span className="flex items-center gap-1">
                  <Bug size={11} />
                  {p.totalBugs}
                </span>

                <span className="flex items-center gap-1">
                  <AlertCircle size={11} />
                  {p.openBugs} open
                </span>

                <span className="ml-auto">{timeAgo(p.createdAt)}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      <CreateProjectModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
      />
    </div>
  );
}
