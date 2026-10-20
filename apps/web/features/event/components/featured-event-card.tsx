"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { CalendarBlankIcon, MapPinIcon } from "@phosphor-icons/react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CountdownCards } from "@/features/event/components/countdown-cards";
import { cn } from "@/lib/utils";
import type { EventDto } from "@/features/event/event-types";

const dateTimeFormat = new Intl.DateTimeFormat("de-AT", {
  weekday: "short",
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});
const priceFormat = new Intl.NumberFormat("de-AT", {
  style: "currency",
  currency: "EUR",
});
const numberFormat = new Intl.NumberFormat("de-AT");

/**
 * Large teaser for one event at the top of the event page.
 *
 * Before the sale opens it counts down to `saleStartsAt` ("Bald erhältlich");
 * once the sale is running it counts down to the event start instead.
 * Ticket count and price are not delivered by the backend yet, so they are
 * optional and only shown when passed in.
 */
export function FeaturedEventCard({
  event,
  ticketCount,
  priceFromCents,
  className,
}: {
  event: EventDto;
  ticketCount?: number;
  priceFromCents?: number;
  className?: string;
}) {
  const startsAt = new Date(event.startsAt);
  const saleStartsAt = new Date(event.saleStartsAt);
  // The current time as state, so rendering stays pure and the badge flips
  // as soon as the sale opens.
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);
  const saleOpen = saleStartsAt.getTime() <= now;

  return (
    <article
      className={cn(
        "flex w-full flex-col overflow-hidden rounded-xl border bg-card text-card-foreground md:flex-row",
        className,
      )}
    >
      <div className="relative aspect-[16/10] w-full shrink-0 md:aspect-auto md:w-2/5">
        <Image
          src={event.imageUrl}
          alt={event.title}
          fill
          priority
          sizes="(min-width: 768px) 40vw, 100vw"
          className="object-cover"
        />
      </div>

      <div className="flex w-full flex-col gap-5 p-5">
        <div className="flex flex-col items-start gap-2">
          <div className="flex items-center gap-2">
            <Badge
              variant={saleOpen ? "default" : "secondary"}
              className="rounded-lg text-xs"
            >
              {saleOpen ? "Im Verkauf" : "Bald erhältlich"}
            </Badge>
            {event.category && (
              <span className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
                {event.category.title}
              </span>
            )}
          </div>

          <h2 className="text-2xl font-extrabold tracking-tight md:text-3xl">
            {event.title}
          </h2>

          <div className="flex flex-col gap-1 text-xs text-muted-foreground sm:flex-row sm:gap-4">
            <span className="flex items-center gap-1.5">
              <CalendarBlankIcon className="size-4" />
              <time dateTime={event.startsAt}>
                {dateTimeFormat.format(startsAt)} Uhr
              </time>
            </span>
            <span className="flex items-center gap-1.5">
              <MapPinIcon className="size-4" />
              {event.venue}, {event.city}
            </span>
          </div>
        </div>

        <div className="flex flex-col gap-2">
          <p className="text-xs text-muted-foreground">
            {saleOpen ? "Das Event beginnt in" : "Verkaufsstart in"}
          </p>
          <CountdownCards
            date={saleOpen ? startsAt : saleStartsAt}
            completedText={saleOpen ? "Das Event läuft" : "Der Verkauf hat begonnen"}
          />
          {!saleOpen && (
            <p className="text-xs text-muted-foreground">
              Verkaufsstart: {dateTimeFormat.format(saleStartsAt)} Uhr
            </p>
          )}
        </div>

        <div className="mt-auto flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          {(ticketCount !== undefined || priceFromCents !== undefined) && (
            <p className="text-xs text-muted-foreground">
              {ticketCount !== undefined && (
                <>{numberFormat.format(ticketCount)} Tickets</>
              )}
              {ticketCount !== undefined && priceFromCents !== undefined && " · "}
              {priceFromCents !== undefined && (
                <>
                  ab{" "}
                  <span className="text-base font-bold text-foreground">
                    {priceFormat.format(priceFromCents / 100)}
                  </span>
                </>
              )}
            </p>
          )}

          <div className="flex items-center gap-2">
            {!saleOpen && (
              <Button
                variant="outline"
                className="h-9 rounded-lg bg-transparent text-xs
                hover:cursor-pointer"
              >
                Erinnerung aktivieren
              </Button>
            )}
            <Button className="h-9 rounded-lg px-4 text-xs hover:cursor-pointer">Zum Event</Button>
          </div>
        </div>
      </div>
    </article>
  );
}
