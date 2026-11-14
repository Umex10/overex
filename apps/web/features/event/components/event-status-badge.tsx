import { CircleIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { EventStatus } from "@/features/event/event-types";

/** Label and dot colour per status. */
const STATUS_LOOK: Record<EventStatus, { label: string; dotClass: string }> = {
  DRAFT: { label: "Entwurf", dotClass: "text-muted-foreground" },
  PUBLISHED: { label: "Veröffentlicht", dotClass: "text-primary" },
  CANCELED: { label: "Abgesagt", dotClass: "text-destructive" },
};

/**
 * Status of an event as a small badge with a coloured dot in front, e.g.
 * "● Entwurf". Used in the live preview of the create-event page and in the
 * list of "Meine Events".
 */
export function EventStatusBadge({
  status,
  className,
}: {
  status: EventStatus;
  className?: string;
}) {
  const { label, dotClass } = STATUS_LOOK[status];

  return (
    <Badge variant="outline" className={cn("rounded-sm", className)}>
      <CircleIcon
        fill="currentColor"
        aria-hidden="true"
        className={cn("size-2", dotClass)}
      />
      <span>{label}</span>
    </Badge>
  );
}
