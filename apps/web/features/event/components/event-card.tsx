import Image from "next/image";
import { CalendarIcon, MapPinIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import Link from "next/link";
import DateFormat from "./date-format";
import TimeFormat from "./time-format";
import PriceFormat from "./price-format";
import EventMetaItem from "./event-meta-item";

/**
 * - `auto`: compact row with a thumbnail on phones, vertical card from md on.
 * - `card`: always the vertical card, also on phones (the "card view").
 */
export type EventCardLayout = "auto" | "card";

export interface EventCardProps {
  id: string;
  imageUrl: string;
  category: string;
  title: string;
  startsAt: string;
  venue: string;
  city: string;
  /** Cheapest ticket price in cent (like `price_cents` in the data model). */
  priceFromCents: number;
  /** Share of tickets still available, 0–100. */
  availablePercent: number;
  /** Optional label on the image, e.g. "Fast ausverkauft". */
  badge?: string;
  layout?: EventCardLayout;
  className?: string;
}

/** Below this share the availability is shown in orange as a warning. */
const LOW_AVAILABILITY_PERCENT = 10;

/**
 * Event teaser for the event list: image with an optional badge, category,
 * title, date, place, the cheapest price and how many tickets are left.
 * Date and price are formatted for Austria (e.g. "Sa., 24.10.2026 · 20:00 Uhr",
 * "€ 59,00").
 *
 * The vertical card is the same layout as the live preview on the
 * create-event page, so an organizer sees exactly what visitors will see.
 */
export function EventCard({
  id,
  imageUrl,
  category,
  title,
  startsAt,
  venue,
  city,
  priceFromCents,
  availablePercent,
  badge,
  layout = "auto",
  className,
}: EventCardProps) {
  const lowAvailability = availablePercent <= LOW_AVAILABILITY_PERCENT;
  // "card" uses the md: look everywhere; "auto" switches at md.
  const isCard = layout === "card";

  return (
    <Link href={`/events/${id}`} data-testid="event-card">
      <article
        className={cn(
          "group flex overflow-hidden rounded-xl border bg-card text-card-foreground transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg hover:cursor-pointer",
          isCard
            ? "flex-col"
            : "flex-row gap-3 p-3 md:flex-col md:gap-0 md:p-0",
          className,
        )}
      >
        <div
          className={cn(
            "relative shrink-0 overflow-hidden",
            isCard
              ? "aspect-video w-full"
              : "aspect-[4/3] w-28 rounded-lg md:aspect-video md:w-auto md:rounded-none",
          )}
        >
          <Image
            src={imageUrl}
            alt={title}
            fill
            sizes={isCard ? "(min-width: 768px) 33vw, 100vw" : "(min-width: 768px) 33vw, 112px"}
            className="object-cover transition-transform duration-300 group-hover:scale-105"
          />
          {badge && (
            <Badge
              variant="secondary"
              className={cn(
                "absolute top-2 left-2 rounded-sm bg-background/90 backdrop-blur",
                !isCard && "hidden md:inline-flex",
              )}
            >
              {badge}
            </Badge>
          )}
        </div>

        <div className={cn("flex min-w-0 flex-col gap-1.5", isCard ? "p-4" : "md:p-4")}>
          {/* Compact row: the badge sits above the title instead of on the image. */}
          {badge && !isCard && (
            <Badge variant="secondary" className="self-start rounded-sm md:hidden">
              {badge}
            </Badge>
          )}
          <p
            className={cn(
              "text-xs font-medium tracking-wide text-muted-foreground uppercase",
              !isCard && "hidden md:block",
            )}
          >
            {category}
          </p>
          <h3 className={cn("line-clamp-2 font-bold", isCard ? "text-lg" : "text-base md:text-lg")}>
            {title}
          </h3>

          {/* Compact row: date and venue on one line. */}
          {!isCard && (
            <p className="truncate text-sm text-muted-foreground md:hidden">
              <DateFormat dateToFormat={startsAt} weekdayType="short" /> · {venue}
            </p>
          )}
          <div
            className={cn(
              "flex-col gap-1 text-sm text-muted-foreground",
              isCard ? "flex" : "hidden md:flex",
            )}
          >
            <EventMetaItem icon={CalendarIcon} className="gap-1.5">
              <DateFormat dateToFormat={startsAt} weekdayType="short" /> ·{" "}
              <TimeFormat timeToFormat={startsAt} />
            </EventMetaItem>
            <EventMetaItem icon={MapPinIcon} className="gap-1.5">
              {venue}, {city}
            </EventMetaItem>
          </div>

          {!isCard && (
            <p className="mt-1 text-sm md:hidden">
              <span className="text-muted-foreground">ab</span>{" "}
              <span className="text-base font-bold">
                <PriceFormat priceInCents={priceFromCents} />
              </span>
            </p>
          )}
        </div>

        <Separator className={cn("mt-auto", !isCard && "hidden md:block")} />

        <div
          className={cn(
            "items-center justify-between px-4 py-3",
            isCard ? "flex" : "hidden md:flex",
          )}
        >
          <p className="text-sm">
            <span className="text-muted-foreground">ab</span>{" "}
            <span className="text-lg font-bold">
              <PriceFormat priceInCents={priceFromCents} />
            </span>
          </p>

          <p
            className={cn(
              "text-sm font-medium",
              lowAvailability ? "text-orange-500" : "text-muted-foreground",
            )}
          >
            Noch {availablePercent} %
          </p>
        </div>
      </article>
    </Link>
  );
}
