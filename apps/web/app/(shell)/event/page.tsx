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

/**
 * The post-login landing page.
 *
 * Reads the authenticated user from `GET /me` (proving the whole auth chain
 * works end to end) and greets them. The confetti celebration is an optional
 * "fun gag" — see components/fun/Celebration.tsx for how to remove it.
 */
export default function EventPage() {
  const { data: user, isLoading: isLoadingMe } = useGetMeQuery();

  const { data: events, isLoading: isLoadingEvents } = useGetEventsQuery();

  const isMobile = useIsMobile();

  if (isLoadingMe || isLoadingEvents) {
    return (
      <div className="flex justify-center py-20">
        <Spinner className="size-8" />
      </div>
    );
  }

  if (!events || events.length === 0) return <p>Keine Events gefunden.</p>;

  // The event whose sale opens next; if every sale has started already, the
  // first event of the list.
  const now = Date.now();
  const featured =
    events
      .filter((e) => new Date(e.saleStartsAt).getTime() > now)
      .sort((a, b) => a.saleStartsAt.localeCompare(b.saleStartsAt))[0] ??
    events[0];

  return (
    <div className="w-full flex flex-col items-center p-2 gap-6">
      {isMobile && <SearchBar></SearchBar>}

      <FeaturedEventCard
        event={featured}
        category="Festival"
        ticketCount={8000}
        priceFromCents={2900}
      />

      <div className="w-full flex flex-col items-start gap-3">
        <div>
          <h3 className="text-lg font-extrabold">All Events</h3>
          <h4 className="text-xs">{`${events.length} Events`}</h4>
        </div>

        <div className="grid w-full grid-cols-1 gap-3 md:grid-cols-3">
          {events?.map((event) => (
            <EventCard
              key={event.id}
              imageUrl={event.imageUrl}
              category={"Festival"}
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
      </div>
    </div>
  );
}
