"use server";

import axios from "axios";
import { cookies } from "next/headers";
import { API_BASE_URL } from "@/lib/config";
import {
  REFRESH_COOKIE,
  REFRESH_COOKIE_MAX_AGE,
  extractRefreshTk,
} from "@/features/auth/refresh-cookie";
import type { SignInFormValues, SignUpFormValues } from "@/features/auth/auth-validation";

/**
 * Pre-configured axios instance for the backend auth API.
 *
 * The base URL is the Spring API root plus the `/auth` prefix, so every call
 * hits `/api/v1/auth/*`.
 */
const authFetch = axios.create({
  baseURL: `${API_BASE_URL}/auth`,
  headers: {
    Accept: "application/json",
  },
});

/**
 * Server action that registers a new account against the backend.
 *
 * Forwards the form payload to `POST /auth/sign-up`, persists the returned
 * refresh-token cookie via Next.js' cookie store (so it stays HTTP-only and is
 * never exposed to client JS), and returns a discriminated result the RTK
 * mutation can branch on.
 *
 * @returns `{ success: true, data }` on success, otherwise `{ success: false, error }`.
 */
export const signUpAction = async (signUpData: SignUpFormValues) => {
  try {
    // `terms` is a UI-only checkbox — strip it so only the backend DTO fields
    // (name, email, phone, password) are sent. The backend assigns the role.
    const { name, email, phone, password } = signUpData;
    const res = await authFetch.post("sign-up", { name, email, phone, password });

    await setRefreshCookie(res.headers["set-cookie"]);

    return { success: true as const, data: res.data };
  } catch (error) {
    return failure(error);
  }
};

/**
 * Server action that authenticates an existing user.
 *
 * Mirrors {@link signUpAction} but targets `POST /auth/sign-in`. The
 * refresh-token cookie returned by the backend is rewritten into the Next.js
 * cookie store so subsequent server requests can read it from `cookies()`.
 *
 * @returns `{ success: true, data }` on success, otherwise `{ success: false, error }`.
 */
export const signInAction = async (signInData: SignInFormValues) => {
  try {
    const res = await authFetch.post("sign-in", signInData);

    await setRefreshCookie(res.headers["set-cookie"]);

    return { success: true as const, data: res.data };
  } catch (error) {
    return failure(error);
  }
};

/**
 * Server action that exchanges the refresh-token cookie for a fresh access
 * token.
 *
 * Reads the HTTP-only `refresh_tk` cookie from the Next.js cookie store and
 * forwards it manually as a `Cookie` header, because server-action axios calls
 * do not share the browser's cookie jar. The endpoint hit is
 * `GET /auth/access-tk`.
 *
 * @returns `{ success: true, data }` carrying the new AuthResponseDto, or
 *          `{ success: false, error }` when the cookie is missing or rejected.
 */
export const getAccessTkAction = async () => {
  try {
    const cookieStore = await cookies();
    const refreshTk = cookieStore.get(REFRESH_COOKIE);

    if (!refreshTk) {
      return {
        success: true as const,
        data: null // guest mode
      };
    }

    const res = await authFetch.get("access-tk", {
      headers: {
        Cookie: `${REFRESH_COOKIE}=${refreshTk.value}`,
      },
    });

    return { success: true as const, data: res.data };
  } catch (error) {
    return failure(error);
  }
};

/**
 * Turns a failed backend call into the `{ success: false, error }` shape. The
 * backend's error body is passed on as is (it carries `status`, `message` and,
 * for sign-up, the field errors); without a response (backend down) a generic
 * message is returned instead of a stack trace.
 */
function failure(error: unknown) {
  if (axios.isAxiosError(error) && error.response) {
    const data = error.response.data;
    // Some 401s come with a plain-text body; normalize to an object.
    return {
      success: false as const,
      error:
        typeof data === "object" && data !== null
          ? data
          : { status: error.response.status, message: String(data || "Request failed.") },
    };
  }
  return {
    success: false as const,
    error: { status: 503, message: "The server is not reachable. Please try again in a moment." },
  };
}

/**
 * Takes the refresh token from the backend's `Set-Cookie` header and sets it
 * again through the Next.js cookie store, with explicit security attributes:
 *
 * - `httpOnly`: client-side JavaScript can never read it (XSS cannot steal it).
 * - `secure` in production: only sent over HTTPS. Off in `next dev`, so plain
 *   http://localhost works. (Browsers also accept secure cookies on localhost,
 *   so `next start` works locally too.)
 * - `sameSite: "lax"`: not sent with cross-site POSTs, which blocks CSRF.
 * - 30 days, matching the token's lifetime in the backend.
 */
async function setRefreshCookie(setCookieHeader: string[] | string | undefined) {
  const value = extractRefreshTk(setCookieHeader);
  if (!value) return;

  const cookieStore = await cookies();
  cookieStore.set({
    name: REFRESH_COOKIE,
    value,
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: REFRESH_COOKIE_MAX_AGE,
    sameSite: "lax",
  });
}

/**
 * Removes the refresh-token cookie from the browser.
 *
 * Used by the sign-out flow; because the backend keeps no server-side session,
 * deleting the cookie is sufficient to terminate the user's authenticated
 * state.
 */
export const deleteRefreshCookie = async () => {
  const cookieStore = await cookies();
  cookieStore.delete(REFRESH_COOKIE);
};
