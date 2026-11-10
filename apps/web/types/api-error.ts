/**
 * Error body of a failed request: the backend's `ApiError`, which every error
 * response (400 to 500) uses, or a generic one built in the frontend when the
 * backend is unreachable. The backend always sets every field; they're
 * optional only because of that frontend fallback.
 */
export interface ApiErrorBody {
  status?: number;
  message?: string;
  /** The requested URI, e.g. "/events/abc-123". */
  path?: string;
  /**
   * One entry per rejected field, e.g. "email already taken". Empty when no
   * single field is to blame (401, 403, 404, 409, 500).
   */
  errors?: { field: string; message: string }[];
}
