/**
 * Base URL of the Overex backend.
 *
 * Read from `NEXT_PUBLIC_BACKEND_URL` with a localhost fallback, so the app
 * runs after a clone with zero configuration and can still be pointed at any
 * backend via the environment variable (see .env.example).
 */
export const BACKEND_URL =
  process.env.NEXT_PUBLIC_BACKEND_URL ?? "http://localhost:8080";

/** Fully-qualified API root, including the Spring servlet context path. */
export const API_BASE_URL = `${BACKEND_URL}/api/v1`;
