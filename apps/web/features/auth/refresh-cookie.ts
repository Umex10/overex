/** Name of the HTTP-only cookie that carries the refresh token. */
export const REFRESH_COOKIE = "refresh_tk";

/** Matches the refresh token's lifetime in the backend (30 days). */
export const REFRESH_COOKIE_MAX_AGE = 30 * 24 * 60 * 60;

/**
 * Reads the refresh token out of the backend's `Set-Cookie` header(s).
 *
 * The server actions call the backend from the Next.js server, so the browser
 * never sees that response; the token has to be picked out here and set again
 * through Next's cookie store.
 *
 * @param setCookie the raw header: Node delivers several cookies as an array
 * @returns the token, or `null` when no non-empty `refresh_tk` cookie is present
 */
export function extractRefreshTk(
  setCookie: string[] | string | undefined,
): string | null {
  const headers = Array.isArray(setCookie) ? setCookie : setCookie ? [setCookie] : [];
  const prefix = `${REFRESH_COOKIE}=`;

  for (const header of headers) {
    const trimmed = header.trim();
    if (!trimmed.startsWith(prefix)) continue;

    // "refresh_tk=<token>; HttpOnly; Path=/" -> "<token>". Cut at the first
    // ";" only, never at "=": a value may contain "=" itself.
    const value = trimmed.slice(prefix.length).split(";")[0].trim();
    return value || null;
  }
  return null;
}
