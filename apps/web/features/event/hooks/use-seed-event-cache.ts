"use client";

import { useCallback } from "react";
import { eventApi } from "@/features/event/event-api";
import type { EventDto } from "@/features/event/event-types";
import { useAppDispatch } from "@/redux/hooks";

/**
 * Returns a function that writes an event the list already has into the
 * `getEvent` cache. Called on click, the detail / edit page then finds it via
 * `useGetEventQuery(id)` right away, without a request and on every device
 * (mouse, keyboard, touch).
 *
 * Only works because the list and `GET /events/{id}` both return `EventDto`.
 * Once they differ, use {@link usePrefetchEvent} instead.
 */
export function useSeedEventCache() {
  const dispatch = useAppDispatch();

  // return useCallback(
  //   (event: EventDto) => {
  //     dispatch(eventApi.util.upsertQueryData("getEvent", event.id, event));
  //   },
  //   [dispatch],
  // );
}
