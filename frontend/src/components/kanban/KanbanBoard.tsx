import { useState, useCallback } from "react";
import {
  DndContext,
  DragEndEvent,
  DragStartEvent,
  DragOverlay,
  closestCorners,
  PointerSensor,
  useSensor,
  useSensors,
} from "@dnd-kit/core";
import {
  SortableContext,
  verticalListSortingStrategy,
  useSortable,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { bugApi } from "@/services/api";
import type { BugSummary, BugStatus } from "@/types";
import { statusColor, priorityColor, timeAgo } from "@/utils/helpers";
import { Badge, Avatar } from "@/components/ui";
import { avatarColor } from "@/utils/helpers";
import clsx from "clsx";

const COLUMNS: { id: BugStatus; label: string; color: string }[] = [
  { id: "OPEN", label: "Open", color: "border-blue-500/40" },
  { id: "IN_PROGRESS", label: "In Progress", color: "border-purple-500/40" },
  { id: "IN_REVIEW", label: "In Review", color: "border-indigo-500/40" },
  { id: "RESOLVED", label: "Resolved", color: "border-green-500/40" },
  { id: "CLOSED", label: "Closed", color: "border-slate-500/40" },
];

interface KanbanCardProps {
  bug: BugSummary;
  onClick: (bug: BugSummary) => void;
}

function KanbanCard({ bug, onClick }: KanbanCardProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: bug.id });
  const style = { transform: CSS.Transform.toString(transform), transition };

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      onClick={() => onClick(bug)}
      className={clsx(
        "bg-slate-900 border border-slate-800 rounded-lg p-3 cursor-pointer hover:border-slate-700 transition-all select-none",
        isDragging && "opacity-50 shadow-xl ring-2 ring-indigo-500",
      )}
    >
      <div className="flex items-center gap-1.5 mb-2 flex-wrap">
        <span className="font-mono text-[10px] text-slate-500">
          {bug.bugNumber}
        </span>
        <Badge className={priorityColor[bug.priority]}>{bug.priority}</Badge>
      </div>
      <p className="text-sm font-medium text-slate-200 leading-snug mb-3 line-clamp-2">
        {bug.title}
      </p>
      <div className="flex items-center justify-between">
        <Badge className={clsx("text-[10px]", statusColor[bug.status])}>
          {bug.status.replace(/_/g, " ")}
        </Badge>
        {bug.assignee?.id && (
          <Avatar
            name={bug.assignee.username}
            size="sm"
            colorClass={avatarColor(bug.assignee.id)}
          />
        )}
      </div>
      <p className="text-[10px] text-slate-600 mt-2">
        {timeAgo(bug.createdAt)}
      </p>
    </div>
  );
}

function KanbanColumn({
  column,
  bugs,
  onBugClick,
}: {
  column: (typeof COLUMNS)[number];
  bugs: BugSummary[];
  onBugClick: (bug: BugSummary) => void;
}) {
  return (
    <div
      className={clsx(
        "flex-shrink-0 w-72 flex flex-col bg-slate-900/50 rounded-xl border-t-2",
        column.color,
      )}
    >
      <div className="flex items-center justify-between px-4 py-3 border-b border-slate-800">
        <h3 className="text-sm font-medium text-slate-300">{column.label}</h3>
        <span className="text-xs text-slate-500 bg-slate-800 px-2 py-0.5 rounded-full">
          {bugs.length}
        </span>
      </div>
      <SortableContext
        items={bugs.map((b) => b.id)}
        strategy={verticalListSortingStrategy}
      >
        <div className="flex-1 p-3 space-y-2.5 overflow-y-auto max-h-[calc(100vh-280px)]">
          {bugs.map((bug) => (
            <KanbanCard key={bug.id} bug={bug} onClick={onBugClick} />
          ))}
          {bugs.length === 0 && (
            <div className="flex items-center justify-center h-20 border-2 border-dashed border-slate-800 rounded-lg">
              <p className="text-xs text-slate-600">Drop here</p>
            </div>
          )}
        </div>
      </SortableContext>
    </div>
  );
}

interface KanbanBoardProps {
  projectId: string;
  bugs: BugSummary[];
  onBugClick: (bug: BugSummary) => void;
}

export default function KanbanBoard({
  projectId,
  bugs,
  onBugClick,
}: KanbanBoardProps) {
  const qc = useQueryClient();
  const [activeBug, setActiveBug] = useState<BugSummary | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
  );

  const updateMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: BugStatus }) =>
      bugApi.update(projectId, id, { status }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["bugs", projectId] });
    },
  });

  const bugsByStatus = useCallback(
    (status: BugStatus) => bugs.filter((b) => b.status === status),
    [bugs],
  );

  const handleDragStart = (event: DragStartEvent) => {
    const bug = bugs.find((b) => b.id === event.active.id);
    if (bug) setActiveBug(bug);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveBug(null);
    if (!over) return;

    const targetColumn = COLUMNS.find(
      (col) =>
        bugsByStatus(col.id).some((b) => b.id === over.id) ||
        col.id === over.id,
    );
    if (!targetColumn) return;

    const draggedBug = bugs.find((b) => b.id === active.id);
    if (!draggedBug || draggedBug.status === targetColumn.id) return;

    updateMutation.mutate({ id: draggedBug.id, status: targetColumn.id });
  };

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      <div className="flex gap-4 overflow-x-auto pb-4">
        {COLUMNS.map((col) => (
          <KanbanColumn
            key={col.id}
            column={col}
            bugs={bugsByStatus(col.id)}
            onBugClick={onBugClick}
          />
        ))}
      </div>
      <DragOverlay>
        {activeBug && (
          <div className="bg-slate-900 border border-indigo-500 rounded-lg p-3 shadow-2xl w-72 opacity-95">
            <p className="text-sm font-medium text-slate-200">
              {activeBug.title}
            </p>
          </div>
        )}
      </DragOverlay>
    </DndContext>
  );
}
