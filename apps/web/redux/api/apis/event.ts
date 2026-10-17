import { apiSlice } from "../apiSlice";

import type { ApiErrorBody } from "@/types/ApiError";
import type { EventsDto } from "@/types/EventsDto";

/**
 * Auth endpoints. They do not call the backend from the browser but go through
 * the server actions, so the refresh token stays in an HTTP-only cookie that
 * client-side JavaScript never sees.
 *
 * Errors are the backend's error body ({@link ApiErrorBody}); the pages read
 * `message` and, for sign-up, `errors` from it.
 */
const eventApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({

    getEvents: builder.query<EventsDto, void>({
      query: () => `/event`,
    }),
  }),
});

export const { useGetEventsQuery } = eventApi;
