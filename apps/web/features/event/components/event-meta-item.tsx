import type { Icon } from "@phosphor-icons/react";
import { cn } from "@/lib/utils";

interface EventMetaItemProps {
  icon: Icon;
  children: React.ReactNode;
  className?: string;
}

/**
 * One line of event info with a muted icon in front, e.g. the date, the start
 * time or the venue.
 */
export default function EventMetaItem({
  icon: IconComponent,
  children,
  className,
}: EventMetaItemProps) {
  return (
    <div className={cn("flex flex-row items-center gap-2", className)}>
      <IconComponent className="size-4 shrink-0 text-muted-foreground" />
      <span>{children}</span>
    </div>
  );
}
