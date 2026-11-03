import Image from "next/image";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import Link from "next/link";
import DateFormat from "./date-format";
import TimeFormat from "./time-format";
import PriceFormat from "./price-format";

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
  className?: string;
}

/** Below this share the availability is shown in orange as a warning. */
const LOW_AVAILABILITY_PERCENT = 10;

/**
 * Event teaser for the event list: image with an optional badge, category,
 * title, date, place, the cheapest price and how many tickets are left.
 * Date and price are formatted for Austria (e.g. "Sa., 24.10.2026 · 20:00 Uhr",
 * "€ 59,00").
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
  className,
}: EventCardProps) {
  const lowAvailability = availablePercent <= LOW_AVAILABILITY_PERCENT;

  return (
    <Link href={`/events/${id}`}
    data-testid="event-card">
      {/* Phones: compact row with a thumbnail on the left. From md on: the
          vertical card with the image on top. */}
      <article
        className={cn(
          "group flex flex-row gap-3 overflow-hidden rounded-xl border bg-card p-3 text-card-foreground transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg hover:cursor-pointer md:flex-col md:gap-0 md:p-0",
          className,
        )}
      >
        <div className="relative aspect-[4/3] w-28 shrink-0 overflow-hidden rounded-lg md:aspect-[3/2] md:w-auto md:rounded-none">
          <Image
            src={imageUrl}
            alt={title}
            fill
            sizes="(min-width: 768px) 33vw, 112px"
            className="object-cover transition-transform duration-300 group-hover:scale-105"
          />
          {badge && (
            <Badge
              variant="secondary"
              className="absolute top-2 left-2 hidden rounded-sm bg-background/90 backdrop-blur md:inline-flex"
            >
              {badge}
            </Badge>
          )}
        </div>

        <div className="flex min-w-0 flex-col gap-1 md:p-3">
          {/* On phones the badge sits above the title instead of on the image. */}
          {badge && (
            <Badge variant="secondary" className="self-start rounded-sm md:hidden">
              {badge}
            </Badge>
          )}
          <p className="hidden text-xs font-medium tracking-wide text-muted-foreground uppercase md:block">
            {category}
          </p>
          <h3 className="line-clamp-2 text-base font-bold md:text-sm">{title}</h3>

          {/* Phones: date and venue on one line. */}
          <p className="truncate text-sm text-muted-foreground md:hidden">
            <DateFormat dateToFormat={startsAt} weekdayType="short" /> · {venue}
          </p>
          <div className="hidden flex-col text-xs text-muted-foreground md:flex">
            {/* One span, so date and time share a line in the flex column. */}
            <span>
              <DateFormat dateToFormat={startsAt} weekdayType="short" /> ·{" "}
              <TimeFormat timeToFormat={startsAt} />
            </span>
            <span>
              {venue}, {city}
            </span>
          </div>

          <p className="mt-1 text-sm md:hidden">
            <span className="text-muted-foreground">ab</span>{" "}
            <span className="text-base font-bold">
              <PriceFormat priceInCents={priceFromCents} />
            </span>
          </p>
        </div>

        <Separator className="mt-auto hidden md:block" />

        <div className="hidden items-center justify-between p-3 md:flex">
          <p className="text-xs">
            <span className="text-muted-foreground">ab</span>{" "}
            <span className="text-base font-bold">
              <PriceFormat priceInCents={priceFromCents} />
            </span>
          </p>

          <p
            className={cn(
              "text-xs font-medium",
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
