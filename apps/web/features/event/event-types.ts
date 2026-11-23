/**
 * Types of the event feature. They mirror the backend's `Event` and `Category`
 * entities; timestamps arrive as ISO-8601 strings (e.g.
 * "2026-10-24T18:00:00Z") and are turned into a `Date` where needed.
 */

/** Lifecycle of an event, like the backend's `Status` enum. */
export type EventStatus = "DRAFT" | "PUBLISHED" | "CANCELED";

/**
 * Selected tab on "Meine Events": one filter, or "" for all events. Only for
 * the UI; the query gets `undefined` instead of "".
 */
export type MyEventFilter = EventStatus | "FINISHED" | "";

/** One category as returned by `GET /categories`. */
export interface CategoryDto {
  id: string;
  title: string;
}

/**
 * One event as returned by `GET /events`, `GET /me/events`, `POST` and
 * `PATCH /events/{id}`; `GET /events/{id}` wraps it in {@link EventDetailsDto}.
 */
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
  status: EventStatus;
  createdAt: string;
  updatedAt: string;
  /**
   * Optimistic-locking counter, raised by the backend on every update. Sent
   * back unchanged with `PATCH /events/{id}`; an outdated value means 409.
   */
  version: number;
}

/** Whether the event may change to one status, and why not if it can't. */
export interface StatusOption {
  disabled: boolean;
  /** Only set when `disabled` is true. */
  why: string | null;
}

export type StatusOptionMap = Record<EventStatus, StatusOption>;

/**
 * `GET /events/{id}`: the event plus, for every target status, whether the
 * backend would allow the change right now. The edit form only shows these.
 */
export interface EventDetailsDto {
  event: EventDto;
  statusOptions: StatusOptionMap;
}

export interface CreateEventDto {
  title: string;
  categoryId: string;
  description: string;
  imageUrl: string;
  startsAt: string;
  saleStartsAt: string;
  venue: string;
  city: string;
  status: EventStatus;
}

export interface CountsOfEvents {
  countOfAllEvents: number;
  countOfCanceledEvents: number;
  countOfDraftEvents: number;
  countOfPublishedEvents: number;
  countOfFinishedEvents: number
}

/**
 * Body of `PATCH /events/{id}`. Same fields as when creating, but without the
 * status: the backend's `UpdateEventDto` doesn't change it. Plus the `version`
 * the editor loaded, so the backend can detect a lost update (409).
 */
export type UpdateEventDto = CreateEventDto & {
  version: number;
};

/** Sort order of `GET /events`, in Spring's `property,direction` format. */
export type EventSort = "startsAt,asc" | "startsAt,desc";

/**
 * Query parameters of `GET /events`. All of them are optional: the backend
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

/**
 * Query parameters of `GET /me/events`: the signed-in admin's own events.
 * The same as {@link EventQuery} plus `status`; missing `status` means all
 * statuses.
 */
export interface MyEventsQuery extends EventQuery {
  filter?: MyEventFilter;
}
