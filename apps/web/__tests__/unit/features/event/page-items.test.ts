import { describe, expect, it } from "vitest";
import { ELLIPSIS, getPageItems } from "@/features/event/page-items";

// --- Tests -----------------------------------------------------------------

// `shows` is what the user sees (1-based, current page in brackets); it is
// only the test name, the check runs on `expected`.
describe("getPageItems", () => {
  it.each([
    { shows: "nothing for 0 pages", page: 0, totalPages: 0, expected: [] },
    { shows: "[1]", page: 0, totalPages: 1, expected: [0] },
    { shows: "[1] 2", page: 0, totalPages: 2, expected: [0, 1] },
    { shows: "1 [2] 3", page: 1, totalPages: 3, expected: [0, 1, 2] },
    { shows: "1 2 [3] 4 5", page: 2, totalPages: 5, expected: [0, 1, 2, 3, 4] },
    {
      shows: "1 … 3 [4] 5",
      page: 3,
      totalPages: 5,
      expected: [0, ELLIPSIS, 2, 3, 4],
    },
    {
      shows: "1 [2] 3 … 5",
      page: 1,
      totalPages: 5,
      expected: [0, 1, 2, ELLIPSIS, 4],
    },
    {
      shows: "[1] 2 … 5",
      page: 0,
      totalPages: 5,
      expected: [0, 1, ELLIPSIS, 4],
    },
    {
      shows: "1 … 4 [5]",
      page: 4,
      totalPages: 5,
      expected: [0, ELLIPSIS, 3, 4],
    },
    // Only page 2 is hidden: even a single page becomes an ellipsis.
    {
      shows: "1 … 3 [4] 5 6",
      page: 3,
      totalPages: 6,
      expected: [0, ELLIPSIS, 2, 3, 4, 5],
    },
    {
      shows: "1 … 4 [5] 6 … 10",
      page: 4,
      totalPages: 10,
      expected: [0, ELLIPSIS, 3, 4, 5, ELLIPSIS, 9],
    },
  ])("shows $shows", ({ page, totalPages, expected }) => {
    // Act
    const items = getPageItems(page, totalPages);

    // Assert
    expect(items).toEqual(expected);
  });
});
