import { ArrowDownIcon, ArrowUpIcon, type LucideIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { CategoryDto, EventSort } from "@/features/event/event-types";
import { CategoryFilter } from "./category-filter";

/**
 * The sort button shows the current order; a click switches to `next`.
 * `label` is read out by screen readers and shown on hover.
 */
const SORT_TOGGLE: Record<
  EventSort,
  { icon: LucideIcon; label: string; next: EventSort }
> = {
  "startsAt,asc": {
    icon: ArrowUpIcon,
    label: "Earliest first",
    next: "startsAt,desc",
  },
  "startsAt,desc": {
    icon: ArrowDownIcon,
    label: "Latest first",
    next: "startsAt,asc",
  },
};

interface EventListFiltersProps {
  categories: CategoryDto[];
  /** "" = all categories. */
  categoryId: string;
  onCategoryChange: (categoryId: string) => void;
  sort: EventSort;
  onSortChange: (sort: EventSort) => void;
  /** Extra buttons right of the sort button, e.g. the view toggle on /events. */
  children?: React.ReactNode;
  className?: string;
}

/**
 * Filter row above an event list: the category chips and the sort button.
 * Used on `/events` and `/my-events`.
 *
 * min-w-0 lets the row get narrower than its content; the category filter
 * takes the remaining space and scrolls, the buttons keep their size
 * (shrink-0) and stay on the right.
 */
export function EventListFilters({
  categories,
  categoryId,
  onCategoryChange,
  sort,
  onSortChange,
  children,
  className,
}: EventListFiltersProps) {
  const sortToggle = SORT_TOGGLE[sort];

  return (
    <div className={cn("min-w-0 flex flex-row items-center gap-4", className)}>
      <CategoryFilter
        categories={categories}
        categoryId={categoryId}
        onChange={onCategoryChange}
        className="flex-1"
      />

      <div className="flex flex-row items-center gap-1">
        <Button
          variant="outline"
          size="icon"
          aria-label={sortToggle.label}
          title={sortToggle.label}
          onClick={() => onSortChange(sortToggle.next)}
          className="shrink-0 rounded-lg hover:cursor-pointer"
        >
          <sortToggle.icon />
        </Button>

        {children}
      </div>
    </div>
  );
}
