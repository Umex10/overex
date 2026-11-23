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
 * Each category is its own round chip; the active one is filled in the brand
 * colour so it stands out from the outlined rest.
 */
const CHIP_CLASS =
  "rounded-full px-4 hover:cursor-pointer data-[state=on]:border-primary data-[state=on]:bg-primary data-[state=on]:text-primary-foreground data-[state=on]:hover:bg-primary/90";

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
        "min-w-0 overflow-x-auto py-1 max-md:[scrollbar-width:none]",
        className,
      )} data-testid="categories-toggle-group"
    >
      <ToggleGroup
        type="single"
        value={categoryId || ALL}
        onValueChange={(value) => onChange(value === ALL ? "" : value)}
        variant="outline"
        spacing={1}
        className="w-max"
      >
        <ToggleGroupItem
          value={ALL}
          className={CHIP_CLASS}
        >
          All
        </ToggleGroupItem>
        {categories.map((c) => (
          <ToggleGroupItem
            key={c.id}
            value={c.id}
            className={CHIP_CLASS}
          >
            {c.title}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
    </div>
  );
}
