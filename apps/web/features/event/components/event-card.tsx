import Image from "next/image";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import Link from "next/link";

export interface EventCardProps {
  id: string;
  imageUrl: string;
  category: string;
  title: string;
  startsAt: Date;
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

const dateFormat = new Intl.DateTimeFormat("de-AT", {
  weekday: "short",
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});
const timeFormat = new Intl.DateTimeFormat("de-AT", {
  hour: "2-digit",
  minute: "2-digit",
});
const priceFormat = new Intl.NumberFormat("de-AT", {
  style: "currency",
  currency: "EUR",
});

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
    <Link href={`/events/${id}`}>
      <article
        className={cn(
          "group flex flex-col overflow-hidden rounded-xl border bg-card text-card-foreground transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg hover:cursor-pointer",
          className,
        )}
      >
        <div className="relative aspect-[3/2] overflow-hidden">
          <Image
            src={imageUrl}
            alt={title}
            fill
            sizes="(min-width: 768px) 33vw, 100vw"
            className="object-cover transition-transform duration-300 group-hover:scale-105"
          />
          {badge && (
            <Badge
              variant="secondary"
              className="absolute top-2 left-2 rounded-lg bg-background/90 backdrop-blur"
            >
              {badge}
            </Badge>
          )}
        </div>

        <div className="flex flex-col gap-1 p-3">
          <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
            {category}
          </p>
          <h3 className="line-clamp-2 text-sm font-bold">{title}</h3>
          <div className="flex flex-col text-xs text-muted-foreground">
            <time dateTime={startsAt.toISOString()}>
              {dateFormat.format(startsAt)} · {timeFormat.format(startsAt)} Uhr
            </time>
            <span>
              {venue}, {city}
            </span>
          </div>
        </div>

        <Separator className="mt-auto" />

        <div className="flex items-center justify-between p-3">
          <p className="text-xs">
            <span className="text-muted-foreground">ab</span>{" "}
            <span className="text-base font-bold">
              {priceFormat.format(priceFromCents / 100)}
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
