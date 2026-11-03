/**
 * One page of a paginated backend response.
 *
 * Mirrors Spring Data's `PagedModel`, which the backend sends because of
 * `spring.data.web.pageable.serialization-mode=via-dto`: the items of the
 * page in `content`, the paging metadata in `page`.
 */
export interface Page<T> {
  content: T[];
  page: {
    /** Requested items per page (the last page may hold fewer). */
    size: number;
    /** 0-based number of this page. */
    number: number;
    /** Items across all pages. */
    totalElements: number;
    totalPages: number;
  };
}
