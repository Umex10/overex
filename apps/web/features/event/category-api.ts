import { apiSlice } from "@/redux/api-slice";
import type { CategoryDto } from "@/features/event/event-types";

/**
 * Category endpoints. They call the backend directly from the browser; the
 * access token is attached by the shared `prepareHeaders` in {@link apiSlice}.
 */
const categoryApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    /** `GET /categories`: all categories. */
    getCategories: builder.query<CategoryDto[], void>({
      query: () => "/categories",
    }),
  }),
});

export const { useGetCategoriesQuery } = categoryApi;
