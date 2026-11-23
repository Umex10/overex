import { apiSlice } from "@/redux/api-slice";
import type {
  CountsOfEvents,
  CreateEventDto,
  EventDetailsDto,
  EventDto,
  EventQuery,
  MyEventsQuery,
  UpdateEventDto,
} from "@/features/event/event-types";
import type { Page } from "@/types/page";

/**
 * Event endpoints. They call the backend directly from the browser; the access
 * token is attached by the shared `prepareHeaders` in {@link apiSlice}.
 */
export const eventApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    /**
     * `GET /events`: one page of events, optionally of one category only.
     *
     * RTK Query leaves `undefined` params out of the URL, so an empty
     * `categoryId` becomes `undefined` and the backend returns all categories.
     */
    getEvents: builder.query<Page<EventDto>, EventQuery>({
      query: ({ categoryId, ...pageable }) => ({
        url: "/events",
        params: { ...pageable, categoryId: categoryId || undefined },
      }),
      // Marks the cached lists as "Event" data, so createEvent can make them
      // outdated (see invalidatesTags below).
      providesTags: ["Event"],
    }),

    /** `GET /events/{id}`: the event and its allowed status changes. */
    getEvent: builder.query<EventDetailsDto, string>({
      query: (eventId) => `/events/${eventId}`,
      providesTags: ["Event"],
    }),

    /**
     * `POST /events`: creates an event (admins only, the backend checks the
     * role). Answers 201 with the stored event, including its new `id`.
     *
     * `invalidatesTags`: every cached getEvents result is refetched the next
     * time it's shown, so the new event appears in the list without a reload.
     */
    createEvent: builder.mutation<EventDto, CreateEventDto>({
      query: (body) => ({
        url: "/events",
        method: "POST",
        body,
      }),
      invalidatesTags: ["Event"],
    }),

    /**
     * `GET /me/events`: one page of the signed-in admin's own events, drafts
     * included. Without `status` the backend returns all statuses; an empty
     * `categoryId` is left out, like in getEvents.
     */
    getMyEvents: builder.query<Page<EventDto>, MyEventsQuery>({
      query: ({ categoryId, ...rest }) => ({
        url: "/me/events",
        params: { ...rest, categoryId: categoryId || undefined },
      }),
      providesTags: ["Event"],
    }),

    getCountsOfEvents: builder.query<CountsOfEvents, void>({
      query: () => ({
        url: "/me/events/counts",
      }),
      providesTags: ["Event"],
    }),

    /**
     * `PATCH /events/{id}`: changes an event (admins only). Answers 200 with
     * the stored event. Refetches every cached event list afterwards.
     */
    updateEvent: builder.mutation<
      EventDto,
      { eventId: string; body: UpdateEventDto }
    >({
      query: ({ eventId, body }) => ({
        url: `/events/${eventId}`,
        method: "PATCH",
        body,
      }),
      invalidatesTags: ["Event"],
    }),

    /**
     * `DELETE /events/{id}`: deletes an event (admins only). Answers 204
     * without a body. Refetches every cached event list afterwards.
     */
    deleteEvent: builder.mutation<void, string>({
      query: (eventId) => ({
        url: `/events/${eventId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Event"],
    }),

    /**
     * Checks in the browser whether an image URL really loads an image.
     * Resolves with the URL on success, fails otherwise.
     *
     * `queryFn` instead of `query`: this must not go through the shared
     * baseQuery. That one adds the access token to every request, and the
     * token must never be sent to a foreign host. A plain `fetch` would also
     * be blocked by CORS on most image hosts. An `Image` element loads the
     * picture like an <img> does: no CORS check, no headers of ours.
     */
    checkImageUrl: builder.mutation<string, string>({
      queryFn: (url) =>
        new Promise((resolve) => {
          const img = new window.Image();
          img.onload = () => resolve({ data: url });
          img.onerror = () =>
            resolve({
              error: {
                status: "CUSTOM_ERROR",
                error: "The image could not be loaded.",
              },
            });
          img.src = url;
        }),
    }),
  }),
});

export const {
  useGetEventsQuery,
  useGetEventQuery,
  useGetMyEventsQuery,
  useGetCountsOfEventsQuery,
  useCreateEventMutation,
  useUpdateEventMutation,
  useDeleteEventMutation,
  useCheckImageUrlMutation,
  usePrefetch,
} = eventApi;
