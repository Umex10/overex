import { useState } from "react";
import type { EventQuery, EventSort } from "@/features/event/event-types";

/** Events per page. */
export const PAGE_SIZE = 10;

/**
 * Page, sort order and category of an event list, without loading anything.
 * Every list (`/events`, `/my-events`) uses it and passes `params` to its own
 * query.
 *
 * The setters carry the list's rules, so a page using the hook can't forget
 * them: a new category or sort order starts again at the first page. Otherwise
 * the user could stay on page 7 of a category that only has 3 pages.
 */
export function useEventListParams() {
  /** 0-based, like the backend's `page` parameter. */
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<EventSort>("startsAt,asc");
  /** "" = all categories. */
  const [categoryId, setCategoryId] = useState("");

  function changeCategory(newCategoryId: string) {
    setCategoryId(newCategoryId);
    setPage(0);
  }

  function changeSort(newSort: EventSort) {
    setSort(newSort);
    setPage(0);
  }

  const params: EventQuery = { page, size: PAGE_SIZE, sort, categoryId };

  return {
    /** Ready to pass to a list query, e.g. `useGetEventsQuery(params)`. */
    params,
    page,
    sort,
    categoryId,
    setPage,
    changeCategory,
    changeSort,
  };
}
