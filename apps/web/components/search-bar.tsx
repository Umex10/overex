import { MagnifyingGlassIcon } from "@phosphor-icons/react/dist/ssr";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

/**
 * Search field with a magnifying glass.
 *
 * By default it takes the full width it gets and may shrink (`min-w-0`), which
 * is what the phone layout needs. Limits such as a maximum width on desktop are
 * passed in from outside via `className` and merged last, so they win
 * (e.g. `className="md:max-w-80"`). All other props go to the `<input>`.
 */
export function SearchBar({
  className,
  ...props
}: React.ComponentProps<typeof Input>) {
  return (
    <div className={cn("relative w-full min-w-0", className)}>
      <MagnifyingGlassIcon
        aria-hidden
        className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
      />
      <Input
        type="search"
        aria-label="Search events"
        placeholder="Events, Artists, Venue..."
        {...props}
        className="w-full rounded-xl pl-8"
      />
    </div>
  );
}
