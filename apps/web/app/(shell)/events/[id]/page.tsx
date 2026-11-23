"use client";
import { useGetEventQuery } from "@/features/event/event-api";
import { setShowSearch } from "@/features/shell/route-context-slice";
import { useAppDispatch } from "@/redux/hooks";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

import { useIsMobile } from "@/hooks/use-mobile";
import BreadcrumbsContainer from "@/features/shell/components/breadcrumbs-container";
import Image from "next/image";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
import { CalendarIcon, ClockIcon, MapPinIcon } from "lucide-react";
import DateFormat from "@/features/event/components/date-format";
import TimeFormat from "@/features/event/components/time-format";
import EventMetaItem from "@/features/event/components/event-meta-item";

export default function EventDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { data, isLoading: isLoadingEvent } = useGetEventQuery(id);

  const dispatch = useAppDispatch();

  const isMobile = useIsMobile();

  const [showMore, setShowMore] = useState<boolean>(false);

  useEffect(() => {
    dispatch(setShowSearch(false));
  }, [dispatch]);

  if (isLoadingEvent) {
    return <p className="w-full text-center">Loading Event...</p>;
  }

  if (!data) {
    return <p className="w-full text-center">{`The Event with the id: ${id} doesn't exist.`}</p>;
  }
  
  const event = data.event;

  return (
    <div className="w-full flex flex-col justify-start gap-4">
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

      <h3 className="line-clamp-2 text-2xl font-extrabold tracking-tight md:text-3xl">{event.title}</h3>

      <div className="w-full flex flex-col justify-start gap-2 text-muted-foreground">
        <EventMetaItem icon={CalendarIcon}>
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
          className={`text-base leading-relaxed text-muted-foreground ${showMore ? "line-clamp-none" : "line-clamp-5"}`}
        >
          {event.description}
        </p>

        <Button
          variant="ghost"
          className="h-auto bg-transparent p-0 py-2 text-primary"
          onClick={() => setShowMore(!showMore)}
        >
          {showMore ? "Weniger anzeigen" : "Mehr Anzeigen"}
        </Button>
      </div>

      <Badge
        variant="secondary"
        className="h-auto w-full justify-start whitespace-normal rounded-xl bg-emerald-400/60 px-4 py-3 text-base"
      >
        <strong>Verkaufsstart: </strong>
        <DateFormat dateToFormat={event.saleStartsAt} weekdayType="long" />
      </Badge>

      <Separator></Separator>
    </div>
  );
}
