"use client";

import { useGetMeQuery } from "@/redux/api/apis/me";
import { Spinner } from "@/components/ui/loader";
// FUN GAG (optional): remove this import and the <Celebration/> usage below to
// turn the dashboard into a plain welcome screen.
import { SearchBar } from "@/components/search-bar";
import { useIsMobile } from "@/hooks/use-mobile";
import { EventCard } from "@/components/event-card";
import { useGetEventsQuery } from "@/redux/api/apis/event";
import { FeaturedEventCard } from "@/components/featured-event-card";
import { useState } from "react";
import { EventSort } from "@/types/EventsDto";

import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";

/**
 * The post-login landing page.
 *
 * Reads the authenticated user from `GET /me` (proving the whole auth chain
 * works end to end) and greets them. The confetti celebration is an optional
 * "fun gag" — see components/fun/Celebration.tsx for how to remove it.
 */
export default function EventPage() {
  const { data: user, isLoading: isLoadingMe } = useGetMeQuery();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sort, setSort] = useState<EventSort>("startsAt,asc");
  const [categoryId, setCategoryId] = useState("");
  const { data: pageEvent, isLoading: isLoadingEvents } = useGetEventsQuery({
    page,
    size,
    sort,
    categoryId,
  });

  const isMobile = useIsMobile();

  if (isLoadingMe || isLoadingEvents) {
    return (
      <div className="flex justify-center py-20">
        <Spinner className="size-8" />
      </div>
    );
  }

  if (!pageEvent || pageEvent.content.length === 0)
    return <p>Keine Events gefunden.</p>;

  const event = pageEvent?.content;
  const totalPages = pageEvent?.page.totalPages;

  const pages: number[] = [];
  for (let p = 0; p < totalPages; p++) {
    // 0 and end
    if (p === 0 || p === totalPages - 1) {
      pages.push(p);
      continue;
    }

    // active - 1 ++ active ++ active + 1
    if (p === page || p === page - 1 || p === page + 1) {
      pages.push(p);
      continue;
    }

    // Hidden page: one ellipsis (-1) stands for the whole gap, so only add it
    // if the previous entry isn't an ellipsis already.
    if (pages[pages.length - 1] !== -1) {
      pages.push(-1);
    }
  }

  // The event whose sale opens next; if every sale has started already, the
  // first event of the list.
  const now = Date.now();
  const featured =
    pageEvent.content
      .filter((e) => new Date(e.saleStartsAt).getTime() > now)
      .sort((a, b) => a.saleStartsAt.localeCompare(b.saleStartsAt))[0] ??
    pageEvent.content[0];

  return (
    <div className="w-full flex flex-col items-center p-2 gap-6">
      {isMobile && <SearchBar></SearchBar>}

      <FeaturedEventCard
        event={featured}
        ticketCount={8000}
        priceFromCents={2900}
      />

      <div className="w-full flex flex-col items-start gap-3">
        <div>
          <h3 className="text-lg font-extrabold">All Events</h3>
          <h4 className="text-xs">{`${pageEvent.page.totalElements} Events`}</h4>
        </div>

        <div className="grid w-full grid-cols-1 gap-3 md:grid-cols-3">
          {pageEvent.content.map((event) => (
            <EventCard
              key={event.id}
              imageUrl={event.imageUrl}
              category={event.category.title}
              title={event.title}
              startsAt={new Date(event.startsAt)}
              venue={event.venue}
              city={event.city}
              priceFromCents={5900}
              availablePercent={2}
              badge="Almost sold"
            />
          ))}
        </div>

        <Pagination>
          <PaginationContent>
            <PaginationItem>
              <PaginationPrevious
                href="#"
                onClick={() => {
                  if (page > 0) {
                    setPage(page - 1);
                  }
                }}
              />
            </PaginationItem>

            {pages.map((p, i) => (
              <PaginationItem key={p === -1 ? `ellipsis-${i}` : p}>
                {p !== -1 ? (
                  <PaginationLink
                    href="#"
                    isActive={page === p}
                    onClick={() => {
                      setPage(p);
                    }}
                  >
                    {p + 1}
                  </PaginationLink>
                ) : (
                  <PaginationEllipsis></PaginationEllipsis>
                )}
              </PaginationItem>
            ))}
            <PaginationItem>
              <PaginationNext
                href="#"
                onClick={() => {
                  if (page < totalPages - 1) {
                    setPage(page + 1);
                  }
                }}
              />
            </PaginationItem>
          </PaginationContent>
        </Pagination>
      </div>
    </div>
  );
}
