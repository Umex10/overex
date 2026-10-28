import { CategoryDto, EventDto, EventSort } from "@/features/event/event-types";
import { PAGE_SIZE, useEventList } from "@/features/event/hooks/use-event-list";
import { Page } from "@/types/page";
import { act, renderHook } from "@testing-library/react";
import { Dispatch, SetStateAction } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const useGetEventsQuery = vi.fn();
const useGetCategoriesQuery = vi.fn();

interface GetEventsArgs {
  page: number;
  size: number;
  sort: EventSort;
  categoryId: string;
}

vi.mock("@/features/event/event-api", () => ({
  useGetEventsQuery: (args: GetEventsArgs) => useGetEventsQuery(args),
}));

vi.mock("@/features/event/category-api", () => ({
  useGetCategoriesQuery: () => useGetCategoriesQuery(),
}));

beforeEach(() => {
  useGetEventsQuery.mockReset();
  useGetCategoriesQuery.mockReset();
});

function renderUseEventList() {
  const { result } = renderHook(() => useEventList());

  return result.current;
}

describe("useEventList", () => {
  it("calls the query with the correct arguments", () => {
    useGetEventsQuery.mockReturnValue({ data: undefined, isLoading: false });
    useGetCategoriesQuery.mockReturnValue({
      data: undefined,
      isLoading: false,
    });

    const result = renderUseEventList();
    const initialSort = "startsAt,asc";
    const initialCategory = "";

    expect(useGetEventsQuery).toHaveBeenLastCalledWith({
      page: 0,
      size: PAGE_SIZE,
      sort: initialSort,
      categoryId: initialCategory,
    });

    act(() => {
      result.setPage(3);
    });

    expect(useGetEventsQuery).toHaveBeenLastCalledWith({
      page: 3,
      size: PAGE_SIZE,
      sort: initialSort,
      categoryId: initialCategory,
    });

    const newSort = "startsAt,desc";
    const newCategory = "festival";

    act(() => {
      result.changeSort(newSort);
    });

    expect(useGetEventsQuery).toHaveBeenLastCalledWith({
      page: 0,
      size: PAGE_SIZE,
      sort: newSort,
      categoryId: initialCategory,
    });

    act(() => {
      result.changeCategory(newCategory);
    });

    expect(useGetEventsQuery).toHaveBeenLastCalledWith({
      page: 0,
      size: PAGE_SIZE,
      sort: newSort,
      categoryId: newCategory,
    });
  });
});
