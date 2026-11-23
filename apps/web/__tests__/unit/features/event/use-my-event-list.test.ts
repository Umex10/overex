import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook } from "@testing-library/react";
import {
  PAGE_SIZE,
  useMyEventList,
} from "@/features/event/hooks/use-my-event-list";

// --- Mocks -----------------------------------------------------------------

const useGetMyEventsQuery = vi.fn();
const useGetCategoriesQuery = vi.fn();

vi.mock("@/features/event/event-api", () => ({
  useGetMyEventsQuery: (...args: unknown[]) => useGetMyEventsQuery(...args),
}));

vi.mock("@/features/event/category-api", () => ({
  useGetCategoriesQuery: (...args: unknown[]) => useGetCategoriesQuery(...args),
}));

// --- Arrange helpers -------------------------------------------------------

/** Both queries answer without data: only the arguments matter here. */
function queriesReturnNoData() {
  useGetMyEventsQuery.mockReturnValue({ data: undefined, isLoading: false });
  useGetCategoriesQuery.mockReturnValue({ data: undefined, isLoading: false });
}

// --- Helpers ---------------------------------------------------------------

function renderUseMyEventList() {
  const { result } = renderHook(() => useMyEventList());

  return result.current;
}

// --- Setup -----------------------------------------------------------------

beforeEach(() => {
  vi.resetAllMocks();
});

// --- Tests -----------------------------------------------------------------

describe("useMyEventList", () => {
  it("asks for the first page, sorted by start date, in all categories and tabs", () => {
    // Arrange
    queriesReturnNoData();

    // Act
    renderUseMyEventList();

    // Assert
    expect(useGetMyEventsQuery).toHaveBeenLastCalledWith({
      page: 0,
      size: PAGE_SIZE,
      sort: "startsAt,asc",
      categoryId: "",
      filter: undefined,
    });
  });

  it("asks for the page that was set", () => {
    // Arrange
    queriesReturnNoData();
    const result = renderUseMyEventList();

    // Act
    act(() => {
      result.setPage(3);
    });

    // Assert
    expect(useGetMyEventsQuery).toHaveBeenLastCalledWith({
      page: 3,
      size: PAGE_SIZE,
      sort: "startsAt,asc",
      categoryId: "",
      filter: undefined,
    });
  });

  it("goes back to the first page when the sort changes", () => {
    // Arrange: not on the first page, so the reset is visible
    queriesReturnNoData();
    const result = renderUseMyEventList();
    act(() => {
      result.setPage(3);
    });

    // Act
    act(() => {
      result.changeSort("startsAt,desc");
    });

    // Assert
    expect(useGetMyEventsQuery).toHaveBeenLastCalledWith({
      page: 0,
      size: PAGE_SIZE,
      sort: "startsAt,desc",
      categoryId: "",
      filter: undefined,
    });
  });

  it("goes back to the first page when the category changes", () => {
    // Arrange: not on the first page, so the reset is visible
    queriesReturnNoData();
    const result = renderUseMyEventList();
    act(() => {
      result.setPage(3);
    });

    // Act
    act(() => {
      result.changeCategory("festival");
    });

    // Assert
    expect(useGetMyEventsQuery).toHaveBeenLastCalledWith({
      page: 0,
      size: PAGE_SIZE,
      sort: "startsAt,asc",
      categoryId: "festival",
      filter: undefined,
    });
  });

  it("goes back to the first page when the filter changes", () => {
    // Arrange: not on the first page, so the reset is visible
    queriesReturnNoData();
    const result = renderUseMyEventList();
    act(() => {
      result.setPage(3);
    });

    // Act
    act(() => {
      result.changeFilter("DRAFT");
    });

    // Assert
    expect(useGetMyEventsQuery).toHaveBeenLastCalledWith({
      page: 0,
      size: PAGE_SIZE,
      sort: "startsAt,asc",
      categoryId: "",
      filter: "DRAFT",
    });
  });
});
