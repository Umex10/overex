"use client";

import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { cn } from "@/lib/utils";
import type { CategoryDto } from "@/features/event/event-types";

/**
 * Toggle value of the "All" item. Radix reads "" as "nothing selected", so an
 * item with the value "" could never be active; "All" needs a value of its own.
 */
const ALL = "all";

/**
 * Category chips above the event list: "All" first, then one per category.
 *
 * `categoryId` "" stands for all categories, the toggle group uses {@link ALL}
 * for that instead. Clicking the active chip again deselects it, which Radix
 * reports as ""; that also falls back to "All", so one chip is always active.
 *
 * When the chips don't fit, they scroll sideways instead of wrapping: the
 * group keeps its full width (`w-max`) and the wrapper around it scrolls. On
 * phones the scrollbar is hidden, swiping is enough there.
 */
export function CategoryFilter({
  categories,
  categoryId,
  onChange,
  className,
}: {
  categories: CategoryDto[];
  categoryId: string;
  onChange: (categoryId: string) => void;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "min-w-0 overflow-x-auto max-md:[scrollbar-width:none]",
        className,
      )}
    >
      <ToggleGroup
        type="single"
        value={categoryId || ALL}
        onValueChange={(value) => onChange(value === ALL ? "" : value)}
        className="w-max bg-gray-800 rounded-lg p-0.5 gap-0.5"
      >
        <ToggleGroupItem
          value={ALL}
          className="rounded-lg hover:cursor-pointer"
        >
          All
        </ToggleGroupItem>
        {categories.map((c) => (
          <ToggleGroupItem
            key={c.id}
            value={c.id}
            className="rounded-lg hover:cursor-pointer"
          >
            {c.title}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
    </div>
  );
}
