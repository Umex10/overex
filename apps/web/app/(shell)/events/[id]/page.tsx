"use client";
import { useGetEventQuery } from "@/features/event/event-api";
import { setBreadcrumbs, setShowSearch } from "@/features/shell/route-context-slice";
import { useAppDispatch } from "@/redux/hooks";
import { useParams, usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { useIsMobile } from "@/hooks/use-mobile";
import BreadcrumbsContainer from "@/features/shell/components/breadcrumbs-container";
import Image from "next/image";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
import { CalendarBlankIcon, ClockIcon, MapPinIcon } from "@phosphor-icons/react";
import DateFormat from "@/features/event/components/date-format";
import TimeFormat from "@/features/event/components/time-format";
import EventMetaItem from "@/features/event/components/event-meta-item";

export default function EventDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { data: event, isLoading: isLoadingEvent } = useGetEventQuery(id);

  const dispatch = useAppDispatch();

  const isMobile = useIsMobile();

  const [showMore, setShowMore] = useState<boolean>(false);

  useEffect(() => {
    dispatch(setShowSearch(false));
  }, [dispatch]);

  if (isLoadingEvent) {
    return <p className="w-full text-center">Loading Event...</p>;
  }

  if (!event) {
    return <p className="w-full text-center">{`The Event with the id: ${id} doesn't exist.`}</p>;
  }

  return (
    <div className="w-full flex flex-col justify-start gap-3">
      {isMobile && <BreadcrumbsContainer></BreadcrumbsContainer>}

      <Image
        src={event.imageUrl}
        alt={event.title}
        width={300}
        height={300}
        className="w-full rounded-xl object-cover transition-transform duration-300 group-hover:scale-105"
      />

      <Badge
        variant="secondary"
        className="rounded-sm bg-primary text-primary-foreground backdrop-blur"
      >
        Almost sold
      </Badge>

      <h3 className="line-clamp-2 text-2xl font-bold">{event.title}</h3>

      <div className="w-full flex flex-col justify-start gap-1 text-slate-400">
        <EventMetaItem icon={CalendarBlankIcon}>
          <DateFormat dateToFormat={event.startsAt} weekdayType="long" />
        </EventMetaItem>

        <EventMetaItem icon={ClockIcon}>
          Beginn <TimeFormat timeToFormat={event.startsAt} />
        </EventMetaItem>

        <EventMetaItem icon={MapPinIcon}>
          {event.venue}, {event.city}
        </EventMetaItem>
      </div>

      <div>
        <p
          className={`text-base text-slate-400 ${showMore ? "line-clamp-none" : "line-clamp-5"}`}
        >
          {event.description}
        </p>

        <Button
          variant="ghost"
          className="bg-transparent text-primary p-0 text-sm"
          onClick={() => setShowMore(!showMore)}
        >
          {showMore ? "Weniger anzeigen" : "Mehr Anzeigen"}
        </Button>
      </div>

      <Badge
        variant="secondary"
        className="w-full rounded-xl p-6 bg-emerald-400/60 text-lg"
      >
        <strong>Verkaufsstart: </strong>
        <DateFormat dateToFormat={event.saleStartsAt} weekdayType="long" />
      </Badge>

      <Separator></Separator>
    </div>
  );
}
