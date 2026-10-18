import {
  createApi,
  fetchBaseQuery,
  type BaseQueryApi,
  type BaseQueryFn,
  type FetchArgs,
  type FetchBaseQueryError,
} from "@reduxjs/toolkit/query/react";
import { API_BASE_URL } from "@/lib/config";

/**
 * Plain fetch against the backend. The shared `prepareHeaders` reads the access
 * token straight from the cached `getAccessTk` query, so every request carries
 * an up-to-date `Authorization: Bearer <tk>` header automatically.
 */
const rawBaseQuery = fetchBaseQuery({
  baseUrl: API_BASE_URL,
  prepareHeaders: (headers, { getState }) => {
    // The access token lives in the cached getAccessTk query result.
    const state = getState() as {
      api: { queries: Record<string, { data?: { accessTk?: string } } | undefined> };
    };
    const tk = state.api.queries["getAccessTk(undefined)"]?.data?.accessTk;

    if (tk) {
      headers.set("authorization", `Bearer ${tk}`);
    }

    return headers;
  },
});

/** The one injected endpoint this file needs, as far as it uses it. */
type GetAccessTkEndpoints = {
  getAccessTk: {
    initiate: (
      arg: undefined,
      options: { forceRefetch: boolean; subscribe: boolean },
    ) => Parameters<BaseQueryApi["dispatch"]>[0];
  };
};

/** One renewal at a time: parallel 401s all wait for the same refresh. */
let refreshing: Promise<unknown> | null = null;

/**
 * The access token lives 15 minutes. When a request comes back 401 (token
 * expired while the page stayed open), this fetches a new one via the refresh
 * cookie and repeats the request once.
 *
 * If the renewal fails too, the `getAccessTk` query ends up in its error state
 * and `AuthProvider` sends the user to `/sign-in`.
 */
const baseQueryWithReauth: BaseQueryFn<
  string | FetchArgs,
  unknown,
  FetchBaseQueryError
> = async (args, api, extraOptions) => {
  let result = await rawBaseQuery(args, api, extraOptions);

  if (result.error?.status === 401) {
    refreshing ??= api
      .dispatch(
        // Injected by features/auth/auth-api.ts; looked up at runtime to avoid a
        // circular import between the two files, hence the cast.
        (apiSlice.endpoints as unknown as GetAccessTkEndpoints).getAccessTk.initiate(undefined, {
          forceRefetch: true,
          subscribe: false,
        }),
      )
      .finally(() => {
        refreshing = null;
      });

    // After a failed refetch RTK still reports the last good `data` (the old
    // token), so success is only "data and no error".
    const renewed = (await refreshing) as { data?: unknown; error?: unknown };
    if (renewed?.data && !renewed.error) {
      result = await rawBaseQuery(args, api, extraOptions);
    }
  }

  return result;
};

/**
 * Root RTK Query slice used by every feature-specific API module (they add
 * their endpoints with `apiSlice.injectEndpoints`).
 */
export const apiSlice = createApi({
  reducerPath: "api",
  baseQuery: baseQueryWithReauth,
  tagTypes: ["Me"],
  endpoints: () => ({}),
});
