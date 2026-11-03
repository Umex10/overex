import type { EventDto } from "@/features/event/event-types";

/**
 * The event whose sale opens next; if every sale has started already, the
 * first event of the list.
 */
export function pickFeaturedEvent(events: EventDto[]): EventDto | undefined {
  const now = Date.now();
  return (
    events
      .filter((e) => new Date(e.saleStartsAt).getTime() > now)
      .sort((a, b) => a.saleStartsAt.localeCompare(b.saleStartsAt))[0] ??
    events[0]
  );
}
