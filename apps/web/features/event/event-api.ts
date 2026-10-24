import { apiSlice } from "@/redux/api-slice";
import type { EventDto, EventQuery } from "@/features/event/event-types";
import type { Page } from "@/types/page";

/**
 * Event endpoints. They call the backend directly from the browser; the access
 * token is attached by the shared `prepareHeaders` in {@link apiSlice}.
 */
const eventApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    /**
     * `GET /event`: one page of events, optionally of one category only.
     *
     * RTK Query leaves `undefined` params out of the URL, so an empty
     * `categoryId` becomes `undefined` and the backend returns all categories.
     */
    getEvents: builder.query<Page<EventDto>, EventQuery>({
      query: ({ categoryId, ...pageable }) => ({
        url: "/event",
        params: { ...pageable, categoryId: categoryId || undefined },
      }),
    }),

    getEvent: builder.query<EventDto, string>({
      query: (eventId) => `/event/${eventId}`
    })
  }),
});

export const { useGetEventsQuery, useGetEventQuery } = eventApi;
