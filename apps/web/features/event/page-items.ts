/** Stands for a gap of hidden pages in the list of {@link getPageItems}. */
export const ELLIPSIS = -1;

/**
 * The 0-based page numbers to show: the first and the last page, the current
 * page and its two neighbours. Each gap of hidden pages in between becomes one
 * {@link ELLIPSIS}.
 *
 * 10 pages, current page 4: `[0, -1, 3, 4, 5, -1, 9]`, shown as
 * `1 … 4 [5] 6 … 10`.
 */
export function getPageItems(page: number, totalPages: number): number[] {
  const items: number[] = [];

  for (let p = 0; p < totalPages; p++) {
    const isFirstOrLast = p === 0 || p === totalPages - 1;
    const isNearCurrent = Math.abs(p - page) <= 1;

    if (isFirstOrLast || isNearCurrent) {
      items.push(p);
    } else if (items[items.length - 1] !== ELLIPSIS) {
      // Only the first hidden page of a gap adds an ellipsis.
      items.push(ELLIPSIS);
    }
  }

  return items;
}
