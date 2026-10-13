import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Next.js 16 middleware (named `proxy` in this version) that gates the app on
 * the presence of the refresh-token cookie.
 *
 * - Unauthenticated visits to a protected route are redirected to `/sign-in`.
 * - Authenticated visits to the landing page (`/`), `/sign-in` or `/sign-up`
 *   are bounced straight to `/dashboard` — if you already have a refresh cookie
 *   there is no reason to show you the marketing/auth pages again. The landing
 *   redirect additionally carries a `?redirected=1` flag so the dashboard can
 *   explain *why* you landed there (see `app/(shell)/dashboard/page.tsx`).
 * - The landing and auth pages stay publicly reachable for logged-out visitors
 *   so they can browse without an infinite redirect loop.
 *
 * This is a cheap presence check (does the cookie exist?), not a validity
 * check — the actual token verification happens on the backend. That keeps the
 * middleware fast and lets it run on every navigation.
 */
export async function proxy(request: NextRequest) {
  const token = request.cookies.get("refresh_tk")?.value;

  const pathname = request.nextUrl.pathname;
  const isAuthPage = pathname === "/sign-in" || pathname === "/sign-up";
  const isLandingPage = pathname === "/";

  // Logged out + visiting a protected page -> go sign in.
  if (!token && !isAuthPage && !isLandingPage) {
    return NextResponse.redirect(new URL("/sign-in", request.url));
  }

  // Logged in + visiting an auth page -> straight to the dashboard.
  if (token && isAuthPage) {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  // Logged in + opening the landing page -> dashboard, flagged so the dashboard
  // can show the "you're already signed in" note (this is the flow you hit by
  // reopening the site at the root URL, or by clicking the Overex logo).
  if (token && isLandingPage) {
    const url = new URL("/dashboard", request.url);
    url.searchParams.set("redirected", "1");
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

/**
 * Matcher that skips assets and API routes so the gate only runs on real page
 * navigations.
 */
export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\..*).*)"],
};
