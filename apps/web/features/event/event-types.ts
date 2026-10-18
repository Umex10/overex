/**
 * Types of the event feature. They mirror the backend's `Event` and `Category`
 * entities; timestamps arrive as ISO-8601 strings (e.g.
 * "2026-10-24T18:00:00Z") and are turned into a `Date` where needed.
 */

/** One category as returned by `GET /category`. */
export interface CategoryDto {
  id: string;
  title: string;
}

/** One event as returned by `GET /event` and `GET /event/{id}`. */
export interface EventDto {
  id: string;
  category: CategoryDto;
  title: string;
  description: string;
  venue: string;
  city: string;
  startsAt: string;
  saleStartsAt: string;
  imageUrl: string;
  status: "DRAFT" | "PUBLISHED" | "CANCELED";
  createdAt: string;
  updatedAt: string;
}

/** Sort order of `GET /event`, in Spring's `property,direction` format. */
export type EventSort = "startsAt,asc" | "startsAt,desc";

/**
 * Query parameters of `GET /event`. All of them are optional: the backend
 * falls back to page 0, 10 events per page, sorted by `startsAt` ascending,
 * across all categories.
 */
export interface EventQuery {
  /** 0-based page number. */
  page?: number;
  size?: number;
  sort?: EventSort;
  /** Only events of this category; empty or missing means all categories. */
  categoryId?: string;
}
