import { Skeleton } from "@/components/ui/skeleton";
import { SECTION_CLASS, SIDE_COLUMN_CLASS } from "@/components/event-editor-layout";
import { cn } from "cn";

/** Label and input of one form field. `tall` for the description textarea. */
function FieldSkeleton({ tall = false }: { tall?: boolean }) {
  return (
    <div className="flex flex-col gap-1">
      <Skeleton className="h-5 w-24 rounded-md" />
      <Skeleton className={cn("w-full rounded-md", tall ? "h-16" : "h-9")} />
    </div>
  );
}

/** Date and time field side by side, like DateTimeFields. */
function DateTimeFieldsSkeleton() {
  return (
    <div className="grid w-full grid-cols-2 gap-3">
      <FieldSkeleton />
      <FieldSkeleton />
    </div>
  );
}

/**
 * Placeholder for {@link EventEditor} while the event is still loading. Same
 * layout as the editor (header, form sections, live preview), so nothing jumps
 * once the real form appears. Uses CSS breakpoints instead of useIsMobile, so
 * the first render needs no JavaScript to look right.
 */
export function EventEditorSkeleton() {
  return (
    <div
      className="flex w-full flex-col gap-3"
      role="status"
      aria-label="Event wird geladen"
    >
      {/* Header: title, and the action buttons from md on */}
      <div className="flex w-full flex-row items-center justify-between">
        <Skeleton className="h-7 w-32 rounded-md" />
        <div className={cn("hidden flex-row gap-2 md:flex", SIDE_COLUMN_CLASS)}>
          <Skeleton className="h-9 flex-1 rounded-md" />
          <Skeleton className="h-9 flex-1 rounded-md" />
        </div>
      </div>

      <div className="flex flex-col gap-3 lg:flex-row lg:items-start">
        <div className="flex w-full min-w-0 flex-col gap-4">
          {/* Grunddaten */}
          <div className={cn(SECTION_CLASS, "flex flex-col gap-3")}>
            <Skeleton className="h-6 w-28 rounded-md" />
            <FieldSkeleton />
            <FieldSkeleton />
            <FieldSkeleton tall />
            <FieldSkeleton />
            <DateTimeFieldsSkeleton />
            <FieldSkeleton />
            <FieldSkeleton />
          </div>

          {/* Verkaufsstart */}
          <div className={cn(SECTION_CLASS, "flex flex-col gap-3")}>
            <Skeleton className="h-6 w-32 rounded-md" />
            <Skeleton className="h-4 w-3/4 rounded-md" />
            <DateTimeFieldsSkeleton />
          </div>
        </div>

        {/* Live preview */}
        <div className={cn(SECTION_CLASS, "flex w-full flex-col gap-2", SIDE_COLUMN_CLASS)}>
          <div className="flex flex-row items-center justify-between">
            <Skeleton className="h-5 w-28 rounded-md" />
            <Skeleton className="h-6 w-16 rounded-full" />
          </div>

          <div className="flex flex-col overflow-hidden rounded-xl border">
            <Skeleton className="aspect-video w-full" />
            <div className="flex flex-col gap-2 p-4">
              <Skeleton className="h-3 w-20 rounded-md" />
              <Skeleton className="h-6 w-3/4 rounded-md" />
              <Skeleton className="h-4 w-1/2 rounded-md" />
              <Skeleton className="h-4 w-2/3 rounded-md" />
            </div>
            <div className="border-t px-4 py-3">
              <Skeleton className="h-4 w-1/2 rounded-md" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
