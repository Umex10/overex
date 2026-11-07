import { apiSlice } from "@/redux/api-slice";
import type { CreateEventDto, EventDto, EventQuery } from "@/features/event/event-types";
import type { Page } from "@/types/page";

/**
 * Event endpoints. They call the backend directly from the browser; the access
 * token is attached by the shared `prepareHeaders` in {@link apiSlice}.
 */
const eventApi = apiSlice.injectEndpoints({
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

    getEvent: builder.query<EventDto, string>({
      query: (eventId) => `/events/${eventId}`
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
  useCreateEventMutation,
  useCheckImageUrlMutation,
} = eventApi;
