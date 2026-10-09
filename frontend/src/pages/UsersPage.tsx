import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Users } from "lucide-react";
import toast from "react-hot-toast";
import clsx from "clsx";

import { userApi } from "@/services/api";
import { useAuthStore } from "@/store/authStore";

import {
  Spinner,
  Badge,
  PageHeader,
  EmptyState,
  Select,
} from "@/components/ui";

import {
  formatDate,
  avatarColor,
  getInitials,
  roleColor,
} from "@/utils/helpers";

import type { UserRole } from "@/types";

const ROLES: UserRole[] = ["ADMIN", "PROJECT_MANAGER", "DEVELOPER", "TESTER"];

export default function UsersPage() {
  const { user: currentUser } = useAuthStore();

  const qc = useQueryClient();

  const { data: users = [], isLoading } = useQuery({
    queryKey: ["users"],
    queryFn: () => userApi.getAll().then((r) => r.data),
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, role }: { id: string; role: string }) =>
      userApi.updateRole(id, role),

    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["users"] });
      toast.success("Role updated");
    },

    onError: () => {
      toast.error("Failed to update role — Admin only");
    },
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner className="text-indigo-500 w-8 h-8" />
      </div>
    );
  }

  const isAdmin = currentUser?.role === "ADMIN";

  return (
    <div>
      <PageHeader
        title="Team"
        subtitle={`${users.length} member${users.length !== 1 ? "s" : ""}`}
      />

      {users.length === 0 ? (
        <EmptyState icon={Users} title="No team members yet" />
      ) : (
        <div className="bg-slate-900 border border-slate-800 rounded-xl divide-y divide-slate-800">
          {users.map((u) => (
            <div key={u.id} className="flex items-center gap-4 px-5 py-4">
              {/* Avatar */}
              <div
                className={clsx(
                  "w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold text-white flex-shrink-0",
                  avatarColor(u.username),
                )}
              >
                {getInitials(u.fullName ?? u.username)}
              </div>

              {/* User info */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium text-slate-200">
                    {u.fullName || u.username}
                  </p>

                  {u.id === currentUser?.id && (
                    <span className="text-xs text-indigo-400 bg-indigo-600/10 px-1.5 py-0.5 rounded">
                      You
                    </span>
                  )}
                </div>

                <p className="text-xs text-slate-500 truncate">{u.email}</p>
              </div>

              {/* Role */}
              <div className="flex items-center gap-3">
                {isAdmin && u.id !== currentUser?.id ? (
                  <Select
                    value={u.role}
                    onChange={(e) =>
                      roleMutation.mutate({
                        id: u.id,
                        role: e.target.value,
                      })
                    }
                    className="py-1 text-xs w-40"
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>
                        {r}
                      </option>
                    ))}
                  </Select>
                ) : (
                  <Badge className={roleColor[u.role]}>
                    {u.role.replace("_", " ")}
                  </Badge>
                )}

                <span className="text-xs text-slate-600 hidden sm:block">
                  Joined {formatDate(u.createdAt)}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {isAdmin && (
        <p className="text-xs text-slate-600 mt-3">
          As Admin you can change team member roles using the dropdown.
        </p>
      )}
    </div>
  );
}
