"use client";

import { useRef, useState } from "react";
import {
  ArrowDownIcon,
  ArrowUpIcon,
  LayoutGridIcon,
  ListIcon,
  type LucideIcon,
} from "lucide-react";
import {
  AnimatePresence,
  MotionConfig,
  motion,
  type Variants,
} from "motion/react";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/loader";
import { SearchBar } from "@/features/shell/components/search-bar";
import { SearchParamToast } from "@/components/search-param-toast";
import { useIsMobile } from "@/hooks/use-mobile";
import { CategoryFilter } from "@/features/event/components/category-filter";
import {
  EventCard,
  type EventCardLayout,
} from "@/features/event/components/event-card";
import { EventPagination } from "@/features/event/components/event-pagination";
import { FeaturedEventCard } from "@/features/event/components/featured-event-card";
import { useEventList } from "@/features/event/hooks/use-event-list";
import type { EventSort } from "@/features/event/event-types";
import { pickFeaturedEvent } from "@/features/event/featured-event";
import { useAppDispatch } from "@/redux/hooks";
import { useEffect } from "react";
import { setShowSearch } from "@/features/shell/route-context-slice";

/**
 * The sort button shows the current order; a click switches to `next`.
 * `label` is read out by screen readers and shown on hover.
 */
const SORT_TOGGLE: Record<
  EventSort,
  { icon: LucideIcon; label: string; next: EventSort }
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
 * Phones only: the view button switches between the compact list ("auto",
 * becomes cards from md on anyway) and the large cards. The button shows the
 * view a click switches to.
 */
const VIEW_TOGGLE: Record<
  EventCardLayout,
  { icon: LucideIcon; label: string; next: EventCardLayout }
> = {
  auto: { icon: LayoutGridIcon, label: "Kartenansicht", next: "card" },
  card: { icon: ListIcon, label: "Listenansicht", next: "auto" },
};

/**
 * Animation when the view changes: the old list fades out, the new one comes
 * in card by card (stagger), each sliding up slightly.
 */
const listVariants: Variants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.04 } },
  exit: { opacity: 0, y: 8, transition: { duration: 0.15 } },
};
const itemVariants: Variants = {
  hidden: { opacity: 0, y: 12, scale: 0.98 },
  visible: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: { type: "spring", stiffness: 380, damping: 30 },
  },
};

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
  const [view, setView] = useState<EventCardLayout>("auto");
  // Start of the list: a page change scrolls back up here, so the user reads
  // the new page from its first event.
  const listStartRef = useRef<HTMLDivElement>(null);

  const dispatch = useAppDispatch();

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
  const viewToggle = VIEW_TOGGLE[view];

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
            <h3 className="text-xl font-extrabold md:text-2xl">All Events</h3>
            <h4 className="text-sm text-muted-foreground">{`${events?.page.totalElements ?? 0} Events`}</h4>
          </div>

          {/* min-w-0 lets this row get narrower than its content; the filter
              takes the remaining space and scrolls, the sort button keeps its
              size (shrink-0) and stays on the right. */}
          <div className="min-w-0 flex flex-row items-center gap-4">
            <CategoryFilter
              categories={categories}
              categoryId={categoryId}
              onChange={changeCategory}
              className="flex-1"
            />

            <div className="flex flex-row gap-1 items-center">
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

              {/* Phones only: from md on the list is cards anyway. */}
              <Button
                variant="outline"
                size="icon"
                aria-label={viewToggle.label}
                title={viewToggle.label}
                onClick={() => setView(viewToggle.next)}
                className="shrink-0 rounded-lg hover:cursor-pointer md:hidden"
              >
                <viewToggle.icon />
              </Button>
            </div>
          </div>
        </div>

        {/* Only the list is replaced when it's empty: filter and sort stay, so
            the user can switch back to another category. */}
        {events && events.content.length > 0 ? (
          // reducedMotion="user": no animation for people who turned motion
          // off in their system settings.
          <MotionConfig reducedMotion="user">
            {/* mode="wait": the old list leaves before the new one enters.
                The key makes a view change count as a new list. */}
            <AnimatePresence mode="wait" initial={false}>
              <motion.div
                key={view}
                variants={listVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                className="grid w-full grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3"
              >
                {events.content.map((event) => (
                  <motion.div key={event.id} variants={itemVariants}>
                    <EventCard
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
                      layout={view}
                    />
                  </motion.div>
                ))}
              </motion.div>
            </AnimatePresence>
          </MotionConfig>
        ) : (
          <p className="py-10 text-center text-muted-foreground">
            Keine Events gefunden.
          </p>
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
