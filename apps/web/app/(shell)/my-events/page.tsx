"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { PlusIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useIsMobile } from "@/hooks/use-mobile";
import BreadcrumbsContainer from "@/features/shell/components/breadcrumbs-container";
import { MyEventItem } from "@/features/event/components/my-event-item";
import { useGetCountsOfEventsQuery } from "@/features/event/event-api";
import type { EventDto, MyEventFilter, StatusFilter } from "@/features/event/event-types";
import { EventPagination } from "@/features/event/components/event-pagination";
import { useMyEventList } from "@/features/event/hooks/use-my-event-list";
import { EventListFilters } from "@/features/event/components/event-list-filters";
import { Spinner } from "@/components/ui/loader";

/**
 * "Meine Events" at `/my-events`: the events the signed-in admin created,
 * filtered by status with the tabs.
 */
export default function MyEventsPage() {
  const isMobile = useIsMobile();

  const { data: countsOfEvents } = useGetCountsOfEventsQuery();

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
    filter,
    setFilter,
  } = useMyEventList();

  const listStartRef = useRef<HTMLDivElement>(null);

  const STATUS_TABS: { value: MyEventFilter; label: string; count: number }[] = [
    {
      value: "",
      label: "Alle",
      count: countsOfEvents?.countOfAllEvents ?? 0,
    },
    {
      value: "DRAFT",
      label: "Entwürfe",
      count: countsOfEvents?.countOfDraftEvents ?? 0,
    },
    {
      value: "PUBLISHED",
      label: "Veröffentlicht",
      count: countsOfEvents?.countOfPublishedEvents ?? 0,
    },
    {
      value: "CANCELED",
      label: "Abgesagt",
      count: countsOfEvents?.countOfCanceledEvents ?? 0,
    },
      {
      value: "FINISHED",
      label: "Beendet",
      count: countsOfEvents?.countOfFinishedEvents ?? 0,
    },
  ];

  function changePage(newPage: number) {
    setPage(newPage);
    listStartRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  }

  return (
    <div className="flex w-full flex-col gap-6">
      {/* On desktop the crumbs sit in the header bar. */}
      {isMobile && <BreadcrumbsContainer />}

      {/* Phone: title, subtitle and button below each other.
          From md on: title left, button right. */}
      <div className="flex w-full flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h3 className="text-xl font-extrabold md:text-2xl">Meine Events</h3>
          <p className="text-sm text-muted-foreground">
            Deine Entwürfe und veröffentlichten Events an einem Ort.
          </p>
        </div>

        {/* A normal link: the browser's back button (router.back) leads
            from /create-event back here. */}
        <Button asChild className="rounded-lg md:p-6 md:text-base">
          <Link href="/create-event">
            <PlusIcon />
            Event erstellen
          </Link>
        </Button>
      </div>

      <div className="flex flex-col gap-2">
        <Tabs
          value={filter}
          onValueChange={(value) => setFilter(value as StatusFilter)}
        >
          {/* Phones: the tabs keep their width and the row scrolls sideways
            (like the category filter on /events). justify-start, otherwise
            the centred row cuts off the first tab on the left. */}
          <TabsList className="w-full justify-start overflow-x-auto max-md:[scrollbar-width:none] sm:w-fit">
            {STATUS_TABS.map((tab) => (
              <TabsTrigger
                key={tab.value}
                value={tab.value}
                className="flex-none px-3 text-sm hover:cursor-pointer"
              >
                {tab.label} ({tab.count})
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
        <EventListFilters
          categories={categories}
          categoryId={categoryId}
          onCategoryChange={changeCategory}
          sort={sort}
          onSortChange={changeSort}
        />
      </div>

      {isLoading && !events && (
        <div className="flex justify-center py-20">
          <Spinner className="size-8" />
        </div>
      )}

      {!isLoading && events && events.content.length > 0 ? (
        <div className="flex w-full flex-col gap-3">
          {events.content.map((event) => (
            <MyEventItem key={event.id} event={event} />
          ))}
        </div>
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
  );
}
