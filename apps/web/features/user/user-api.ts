import { User } from "@/features/user/user-types";
import { apiSlice } from "@/redux/api-slice";

/**
 * Endpoint for the protected `GET /me` route.
 *
 * A normal authenticated request: the access token is attached automatically
 * by the shared `prepareHeaders` in {@link apiSlice}. `AuthProvider` loads the
 * current user with it before any protected page renders.
 */
const meApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getMe: builder.query<User, void>({
      query: () => `/me`,
      providesTags: ["Me"],
    }),
  }),
});

export const { useGetMeQuery } = meApi;
