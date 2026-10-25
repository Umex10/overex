"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { BellIcon, CalendarBlankIcon, MapPinIcon } from "@phosphor-icons/react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CountdownCards } from "@/features/event/components/countdown-cards";
import { cn } from "@/lib/utils";
import type { EventDto } from "@/features/event/event-types";
import Link from "next/link";
import DateFormat from "@/features/event/components/date-format";
import TimeFormat from "@/features/event/components/time-format";
import PriceFormat from "@/features/event/components/price-format";
import EventMetaItem from "@/features/event/components/event-meta-item";

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
    <Link href={`/events/${event.id}`} className="w-full">
      <article
        className={cn(
          "flex flex-col overflow-hidden rounded-xl border bg-card text-card-foreground md:flex-row",
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
                className="rounded-sm text-xs"
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
              <EventMetaItem icon={CalendarBlankIcon} className="gap-1.5">
                <DateFormat dateToFormat={event.startsAt} weekdayType="short" />{" "}
                · <TimeFormat timeToFormat={event.startsAt} />
              </EventMetaItem>
              <EventMetaItem icon={MapPinIcon} className="gap-1.5">
                {event.venue}, {event.city}
              </EventMetaItem>
            </div>
          </div>

          <div className="flex flex-col gap-2">
             <p className="text-xs text-muted-foreground">
                Verkaufsstart:{" "}
                <DateFormat dateToFormat={event.saleStartsAt} weekdayType="short" />{" "}
                · <TimeFormat timeToFormat={event.saleStartsAt} />
              </p>
            <CountdownCards
              date={saleOpen ? startsAt : saleStartsAt}
              completedText={
                saleOpen ? "Das Event läuft" : "Der Verkauf hat begonnen"
              }
            />
          </div>

          <div className="mt-auto flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            {(ticketCount !== undefined || priceFromCents !== undefined) && (
              <p className="text-xs text-muted-foreground">
                {ticketCount !== undefined && (
                  <>{numberFormat.format(ticketCount)} Tickets</>
                )}
                {ticketCount !== undefined &&
                  priceFromCents !== undefined &&
                  " · "}
                {priceFromCents !== undefined && (
                  <>
                    ab{" "}
                    <span className="text-base font-bold text-foreground">
                      <PriceFormat priceInCents={priceFromCents} />
                    </span>
                  </>
                )}
              </p>
            )}

            <div className="w-full flex flex-col items-center gap-2">
              <Button className="w-full h-9 rounded-lg px-4 text-xs hover:cursor-pointer">
                Zum Event
              </Button>

              {!saleOpen && (
                <Button
                  variant="outline"
                  className="w-full h-9 rounded-lg bg-transparent text-xs
                hover:cursor-pointer"
                >
                  <BellIcon />
                  Erinnerung aktivieren
                </Button>
              )}
            </div>
          </div>
        </div>
      </article>
    </Link>
  );
}
