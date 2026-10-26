import { configureStore } from "@reduxjs/toolkit";
import { apiSlice } from "./api-slice";
import { routeContextSlice } from "./route-context-slice";

/**
 * Creates the Redux store for the web app.
 *
 * A factory instead of a module-level singleton: on the server, Next.js renders
 * the pages of all users in the same process, and one shared store could carry
 * one user's cached data (e.g. the access token) into another user's render.
 * `StoreProvider` creates exactly one store per browser tab.
 *
 * Only the shared `apiSlice` is mounted; feature endpoints inject themselves
 * into it via `apiSlice.injectEndpoints`, so there is one cache for the app.
 */
export const makeStore = () =>
  configureStore({
    reducer: {
      [apiSlice.reducerPath]: apiSlice.reducer,
      routeContext: routeContextSlice.reducer
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(apiSlice.middleware),
  });

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];
