import { useState } from "react";
import { useGetCategoriesQuery } from "@/features/event/category-api";
import { useGetEventsQuery } from "@/features/event/event-api";
import type { EventSort } from "@/features/event/event-types";

/** Events per page. */
export const PAGE_SIZE = 10;

/**
 * State and data of the event list: the selected page, sort order and
 * category, the matching page of events and all categories for the filter.
 *
 * The setters carry the list's rules, so a page using the hook can't forget
 * them: a new category or sort order starts again at the first page. Otherwise
 * the user could stay on page 7 of a category that only has 3 pages.
 */
export function useEventList() {
  /** 0-based, like the backend's `page` parameter. */
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<EventSort>("startsAt,asc");
  /** "" = all categories. */
  const [categoryId, setCategoryId] = useState("");

  const events = useGetEventsQuery({ page, size: PAGE_SIZE, sort, categoryId });
  const categories = useGetCategoriesQuery();

  function changeCategory(newCategoryId: string) {
    setCategoryId(newCategoryId);
    setPage(0);
  }

  function changeSort(newSort: EventSort) {
    setSort(newSort);
    setPage(0);
  }

  return {
    /** The current page of events; `undefined` until the first response. */
    events: events.data,
    categories: categories.data ?? [],
    /** Only true on the very first load: on a page change the old events stay visible. */
    isLoading: events.isLoading || categories.isLoading,
    page,
    sort,
    categoryId,
    setPage,
    changeCategory,
    changeSort,
  };
}
