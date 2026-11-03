import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook } from "@testing-library/react";
import { PAGE_SIZE, useEventList } from "@/features/event/hooks/use-event-list";

// --- Mocks -----------------------------------------------------------------

const useGetEventsQuery = vi.fn();
const useGetCategoriesQuery = vi.fn();

vi.mock("@/features/event/event-api", () => ({
  useGetEventsQuery: (...args: unknown[]) => useGetEventsQuery(...args),
}));

vi.mock("@/features/event/category-api", () => ({
  useGetCategoriesQuery: (...args: unknown[]) => useGetCategoriesQuery(...args),
}));

// --- Arrange helpers -------------------------------------------------------

/** Both queries answer without data: only the arguments matter here. */
function queriesReturnNoData() {
  useGetEventsQuery.mockReturnValue({ data: undefined, isLoading: false });
  useGetCategoriesQuery.mockReturnValue({ data: undefined, isLoading: false });
}

// --- Helpers ---------------------------------------------------------------

function renderUseEventList() {
  const { result } = renderHook(() => useEventList());

  return result.current;
}

// --- Setup -----------------------------------------------------------------

beforeEach(() => {
  vi.resetAllMocks();
});

// --- Tests -----------------------------------------------------------------

describe("useEventList", () => {
  it("asks for the first page, sorted by start date, in all categories", () => {
    // Arrange
    queriesReturnNoData();

    // Act
    renderUseEventList();

    // Assert
    expect(useGetEventsQuery).toHaveBeenLastCalledWith({
      page: 0,
      size: PAGE_SIZE,
      sort: "startsAt,asc",
      categoryId: "",
    });
  });

  it("asks for the page that was set", () => {
    // Arrange
    queriesReturnNoData();
    const result = renderUseEventList();

    // Act
    act(() => {
      result.setPage(3);
    });

    // Assert
    expect(useGetEventsQuery).toHaveBeenLastCalledWith({
      page: 3,
      size: PAGE_SIZE,
      sort: "startsAt,asc",
      categoryId: "",
    });
  });

  it("goes back to the first page when the sort changes", () => {
    // Arrange: not on the first page, so the reset is visible
    queriesReturnNoData();
    const result = renderUseEventList();
    act(() => {
      result.setPage(3);
    });

    // Act
    act(() => {
      result.changeSort("startsAt,desc");
    });

    // Assert
    expect(useGetEventsQuery).toHaveBeenLastCalledWith({
      page: 0,
      size: PAGE_SIZE,
      sort: "startsAt,desc",
      categoryId: "",
    });
  });

  it("goes back to the first page when the category changes", () => {
    // Arrange: not on the first page, so the reset is visible
    queriesReturnNoData();
    const result = renderUseEventList();
    act(() => {
      result.setPage(3);
    });

    // Act
    act(() => {
      result.changeCategory("festival");
    });

    // Assert
    expect(useGetEventsQuery).toHaveBeenLastCalledWith({
      page: 0,
      size: PAGE_SIZE,
      sort: "startsAt,asc",
      categoryId: "festival",
    });
  });
});
