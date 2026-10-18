/**
 * One event as returned by the backend (`GET /event`, `GET /event/{id}`).
 *
 * Mirrors the backend's `Event` entity. Timestamps arrive as ISO-8601 strings
 * (e.g. "2026-10-24T18:00:00Z"); turn them into a `Date` where needed.
 */
export interface EventCategoryDto {
  id: string;
  title: string;
}

export interface EventDto {
  id: string;
  category: EventCategoryDto;
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

export type EventSort = "startsAt,asc" | "startsAt,desc";

export interface PageAble {
  page?: number;
  size?: number;
  sort?: EventSort;
  categoryId?: string;
}

type Page = {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
};

export interface PageEvent {
  content: EventDto[];
  page: Page;
}

/** Response of `GET /event`: all events. */
export type EventsDto = EventDto[];
