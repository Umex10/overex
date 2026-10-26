"use client";

import { useRef } from "react";
import { ArrowDownIcon, ArrowUpIcon, type Icon } from "@phosphor-icons/react";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/loader";
import { SearchBar } from "@/features/shell/components/search-bar";
import { SearchParamToast } from "@/components/search-param-toast";
import { useIsMobile } from "@/hooks/use-mobile";
import { CategoryFilter } from "@/features/event/components/category-filter";
import { EventCard } from "@/features/event/components/event-card";
import { EventPagination } from "@/features/event/components/event-pagination";
import { FeaturedEventCard } from "@/features/event/components/featured-event-card";
import { useEventList } from "@/features/event/hooks/use-event-list";
import type { EventDto, EventSort } from "@/features/event/event-types";
import { useAppDispatch } from "@/redux/hooks";
import { useEffect } from "react";
import { setShowSearch } from "@/features/shell/route-context-slice";
import useBreadcrumbs from "@/features/shell/hooks/use-breadcrumbs";

/**
 * The sort button shows the current order; a click switches to `next`.
 * `label` is read out by screen readers and shown on hover.
 */
const SORT_TOGGLE: Record<
  EventSort,
  { icon: Icon; label: string; next: EventSort }
> = {
  "startsAt,asc": {
    icon: ArrowUpIcon,
    label: "Earliest first",
    next: "startsAt,desc",
  },
  "startsAt,desc": {
    icon: ArrowDownIcon,
    label: "Latest first",
    next: "startsAt,asc",
  },
};

/**
 * The event whose sale opens next; if every sale has started already, the
 * first event of the list.
 */
function pickFeaturedEvent(events: EventDto[]): EventDto | undefined {
  const now = Date.now();
  return (
    events
      .filter((e) => new Date(e.saleStartsAt).getTime() > now)
      .sort((a, b) => a.saleStartsAt.localeCompare(b.saleStartsAt))[0] ??
    events[0]
  );
}

/**
 * Event overview at `/events`: a featured event on top, below it the event list
 * with category filter, sort order and pagination.
 *
 * Filter, sort and page state live in {@link useEventList}; this page only
 * puts the parts together.
 */
export default function EventPage() {
  const {
    events,
    categories,
    isLoading,
    page,
    sort,
    categoryId,
    setPage,
    changeCategory,
    changeSort,
  } = useEventList();
  const isMobile = useIsMobile();
  // Start of the list: a page change scrolls back up here, so the user reads
  // the new page from its first event.
  const listStartRef = useRef<HTMLDivElement>(null);

  const dispatch = useAppDispatch();

  useBreadcrumbs();

  useEffect(() => {
    dispatch(setShowSearch(false));
  }, [dispatch]);

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner className="size-8" />
      </div>
    );
  }

  const featured = events && pickFeaturedEvent(events.content);
  const sortToggle = SORT_TOGGLE[sort];

  function changePage(newPage: number) {
    setPage(newPage);
    listStartRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  }

  return (
    <div className="w-full flex flex-col items-center gap-6">
      {/* Set by the proxy when a signed-in user opens the landing page. */}
      <SearchParamToast
        param="redirected"
        value="1"
        message="Du bist bereits angemeldet."
      />

      {isMobile && <SearchBar />}

      {featured && (
        <FeaturedEventCard
          event={featured}
          ticketCount={8000}
          priceFromCents={2900}
        />
      )}

      <div className="w-full flex flex-col items-start gap-3">
        {/* Phone: title on top, filter and sort in one row below it.
            Desktop: title left, filter and sort right. */}
        <div className="w-full flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div className="shrink-0" ref={listStartRef}>
            <h3 className="text-lg font-extrabold">All Events</h3>
            <h4 className="text-xs">{`${events?.page.totalElements ?? 0} Events`}</h4>
          </div>

          {/* min-w-0 lets this row get narrower than its content; the filter
              takes the remaining space and scrolls, the sort button keeps its
              size (shrink-0) and stays on the right. */}
          <div className="min-w-0 flex flex-row items-center gap-2">
            <CategoryFilter
              categories={categories}
              categoryId={categoryId}
              onChange={changeCategory}
              className="flex-1"
            />

            <Button
              variant="outline"
              size="icon"
              aria-label={sortToggle.label}
              title={sortToggle.label}
              onClick={() => changeSort(sortToggle.next)}
              className="shrink-0 rounded-lg hover:cursor-pointer"
            >
              <sortToggle.icon />
            </Button>
          </div>
        </div>

        {/* Only the list is replaced when it's empty: filter and sort stay, so
            the user can switch back to another category. */}
        {events && events.content.length > 0 ? (
          <div className="grid w-full grid-cols-1 gap-3 md:grid-cols-3">
            {events.content.map((event) => (
              <EventCard
                key={event.id}
                id={event.id}
                imageUrl={event.imageUrl}
                category={event.category.title}
                title={event.title}
                startsAt={event.startsAt}
                venue={event.venue}
                city={event.city}
                priceFromCents={5900}
                availablePercent={2}
                badge="Almost sold"
              />
            ))}
          </div>
        ) : (
          <p>Keine Events gefunden.</p>
        )}

        {events && (
          <EventPagination
            page={page}
            totalPages={events.page.totalPages}
            onPageChange={changePage}
          />
        )}
      </div>
    </div>
  );
}
