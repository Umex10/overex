import { useGetCategoriesQuery } from "@/features/event/category-api";
import { useGetMyEventsQuery } from "@/features/event/event-api";
import { useEventListParams } from "@/features/event/hooks/use-event-list-params";
import { useState } from "react";
import type { StatusFilter } from "@/features/event/event-types";

export { PAGE_SIZE } from "@/features/event/hooks/use-event-list-params";

/**
 * State and data of the public event list at `/events`: page, sort order and
 * category (from {@link useEventListParams}), the matching page of events and
 * all categories for the filter.
 */
export function useMyEventList() {
  const { params, ...list } = useEventListParams();
  const [status, setStatus] = useState<StatusFilter>("");

  // "" (tab "Alle") becomes undefined: RTK Query leaves it out of the URL, and
  // the type fits MyEventsQuery, which only knows real statuses.
  const events = useGetMyEventsQuery({ ...params, status: status || undefined });
  const categories = useGetCategoriesQuery();

  return {
    /** The current page of events; `undefined` until the first response. */
    events: events.data,
    categories: categories.data ?? [],
    /** Only true on the very first load: on a page change the old events stay visible. */
    isLoading: events.isLoading || categories.isLoading,
    ...list,
    status,
    setStatus
  };
}
