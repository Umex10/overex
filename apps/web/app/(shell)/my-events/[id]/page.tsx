"use client";

import EventEditor from "@/components/EventEditor";
import { EventEditorSkeleton } from "@/components/EventEditorSkeleton";
import { useGetEventQuery } from "@/features/event/event-api";
import { useParams } from "next/navigation";

export default function MyEventItem() {
  const { id } = useParams<{ id: string }>();
  const { data, isLoading } = useGetEventQuery(id);

  if (isLoading) return <EventEditorSkeleton />;
  if (!data) return <p>Event nicht gefunden.</p>;

  return <EventEditor mode="EDIT" formData={data} />;
}
