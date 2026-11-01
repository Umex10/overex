"use client";

import type { MouseEvent } from "react";
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";
import { ELLIPSIS, getPageItems } from "@/features/event/page-items";

/**
 * Page navigation below the event list: previous, the page numbers from
 * {@link getPageItems} (shown 1-based) and next. Hidden when there is only one
 * page.
 */
export function EventPagination({
  page,
  totalPages,
  onPageChange,
}: {
  /** 0-based current page. */
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;

  /**
   * The shadcn pagination links are `<a href="#">`: without `preventDefault`
   * the browser would follow the link, jump to the top of the page and add
   * "#" to the URL. Targets outside the existing pages are ignored.
   */
  function goTo(event: MouseEvent<HTMLAnchorElement>, target: number) {
    event.preventDefault();
    if (target < 0 || target > totalPages - 1 || target === page) return;
    onPageChange(target);
  }

  return (
    <Pagination>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious href="#" onClick={(e) => goTo(e, page - 1)} />
        </PaginationItem>

        {getPageItems(page, totalPages).map((p, i) => (
          // Two ellipses can appear at once, so they get their position as key.
          <PaginationItem key={p === ELLIPSIS ? `ellipsis-${i}` : p}>
            {p === ELLIPSIS ? (
              <PaginationEllipsis />
            ) : (
              <PaginationLink
                href="#"
                isActive={p === page}
                onClick={(e) => goTo(e, p)}
              >
                {p + 1}
              </PaginationLink>
            )}
          </PaginationItem>
        ))}

        <PaginationItem>
          <PaginationNext href="#" onClick={(e) => goTo(e, page + 1)} />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
}
