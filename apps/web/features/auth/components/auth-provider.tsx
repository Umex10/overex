"use client";

import React, { useEffect } from "react";
import { useGetAccessTkQuery } from "@/features/auth/auth-api";
import { useGetMeQuery } from "@/features/user/user-api";
import { deleteRefreshCookie } from "@/features/auth/auth-actions";
import { FullPageLoader } from "@/components/ui/loader";

/**
 * Gate for every protected page under `(shell)`.
 *
 * 1. Exchanges the HTTP-only refresh cookie for an access token (silent
 *    refresh), then loads the current user via `GET /me`.
 * 2. Shows a loader until both are there, so pages can assume a signed-in user.
 * 3. If the session cannot be restored (refresh rejected, or the account is
 *    gone), it deletes the stale cookie and sends the user to `/sign-in`.
 *
 * Step 3 matters because `proxy.ts` only checks that the cookie *exists*. A
 * cookie the backend rejects (expired, signed with an old key) would otherwise
 * leave the user on an empty dashboard, and `/sign-in` would keep bouncing
 * them back because the cookie is still there.
 */
const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const tk = useGetAccessTkQuery(undefined);
  const me = useGetMeQuery(undefined, { skip: !tk.data });

  // 401 = token not accepted, 404 = the account was deleted meanwhile.
  const meStatus = (me.error as { status?: number } | undefined)?.status;
  const sessionLost =
    tk.isError || (me.isError && (meStatus === 401 || meStatus === 404));

  useEffect(() => {
    if (!sessionLost) return;
    (async () => {
      await deleteRefreshCookie();
      // Full navigation (not router.push) so the Redux cache with the old
      // token is thrown away as well.
      window.location.replace("/sign-in?session=expired");
    })();
  }, [sessionLost]);

  if (sessionLost) {
    return <FullPageLoader label="Your session has expired …" />;
  }

  if (tk.isLoading || (tk.data && me.isLoading)) {
    return <FullPageLoader label="One moment, signing you in …" />;
  }

  return <>{children}</>;
};

export default AuthProvider;
