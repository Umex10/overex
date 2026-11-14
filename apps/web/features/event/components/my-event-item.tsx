"use client"

import Image from "next/image";
import { CalendarIcon, MapPinIcon, PencilIcon, Trash2Icon } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { EventDto } from "@/features/event/event-types";
import DateFormat from "./date-format";
import EventMetaItem from "./event-meta-item";
import { EventStatusBadge } from "./event-status-badge";
import TimeFormat from "./time-format";
import Link from "next/link";

interface MyEventItemProps {
  event: EventDto;
  onEdit?: (eventId: string) => void;
  onDelete?: (eventId: string) => void;
}

/**
 * One row in "Meine Events": thumbnail, category, title, date and place, the
 * status and the buttons to edit or delete the event.
 *
 * Phones: image and text side by side, status and buttons in their own row
 * below. From sm on everything sits in one row, status and buttons on the right.
 */
export function MyEventItem({ event, onEdit, onDelete }: MyEventItemProps) {
  return (
    <Link href={`/my-events/${event.id}`}>
      <article
        data-testid="my-event-item"
        className="flex flex-col gap-3 rounded-xl border bg-card p-3 text-card-foreground sm:flex-row sm:items-center sm:gap-4"
      >
        <div className="flex min-w-0 flex-1 flex-row gap-3 sm:gap-4">
          <div className="relative aspect-4/3 w-24 shrink-0 overflow-hidden rounded-lg sm:aspect-video sm:w-36">
            <Image
              src={event.imageUrl}
              alt=""
              fill
              sizes="(min-width: 640px) 144px, 96px"
              className="object-cover"
            />
          </div>

          <div className="flex min-w-0 flex-col gap-1">
            <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
              {event.category.title}
            </p>
            <h3 className="line-clamp-2 font-bold sm:text-lg">{event.title}</h3>

            <div className="flex flex-col gap-1 text-sm text-muted-foreground md:flex-row md:gap-4">
              <EventMetaItem icon={CalendarIcon} className="gap-1.5">
                <DateFormat dateToFormat={event.startsAt} weekdayType="short" />{" "}
                · <TimeFormat timeToFormat={event.startsAt} />
              </EventMetaItem>
              <EventMetaItem icon={MapPinIcon} className="min-w-0 gap-1.5">
                {event.venue}, {event.city}
              </EventMetaItem>
            </div>
          </div>
        </div>

        <div className="flex shrink-0 flex-row items-center justify-between gap-3 border-t pt-3 sm:justify-end sm:border-t-0 sm:pt-0">
          <EventStatusBadge status={event.status} />

          <div className="flex flex-row gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => onEdit?.(event.id)}
              className="hover:cursor-pointer"
            >
              <PencilIcon />
              Bearbeiten
            </Button>
            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              aria-label={`Event „${event.title}“ löschen`}
              title="Löschen"
              onClick={() => onDelete?.(event.id)}
              className="text-destructive hover:cursor-pointer hover:text-destructive"
            >
              <Trash2Icon />
            </Button>
          </div>
        </div>
      </article>
    </Link>
  );
}
