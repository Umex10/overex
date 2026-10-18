/**
 * Error body as the auth server actions hand it to the pages: the backend's
 * `ApiError`/`ErrorResponse` envelope, or a generic one when the backend is
 * unreachable.
 */
export interface ApiErrorBody {
  status?: number;
  message?: string;
  /** Sign-up only: one entry per rejected field, e.g. "email already taken". */
  errors?: { field: string; message: string }[];
}
