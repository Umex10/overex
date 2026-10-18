import { apiSlice } from "../apiSlice";

import type { ApiErrorBody } from "@/types/ApiError";
import type { EventsDto, PageAble, PageEvent } from "@/types/EventsDto";

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

    getEvents: builder.query<PageEvent, PageAble>({
      query: (pageable) => ({
        url: `/event?page=${pageable.page}&size=${pageable.size}&sort=${pageable.sort}&categoryId=${pageable.categoryId}`,
      }) 
    }),
  }),
});

export const { useGetEventsQuery } = eventApi;
