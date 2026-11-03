"use client";

import React, { useEffect } from "react";
import { useGetAccessTkQuery } from "@/features/auth/auth-api";
import { useGetMeQuery } from "@/features/user/user-api";
import { deleteRefreshCookie } from "@/features/auth/auth-actions";
import { FullPageLoader } from "@/components/ui/loader";
import { Button } from "@/components/ui/button";

/**
 * Gate for every protected page under `(shell)`.
 *
 * 1. Exchanges the HTTP-only refresh cookie for an access token (silent
 *    refresh), then loads the current user via `GET /me`.
 * 2. Shows a loader until both are there, so pages can assume a signed-in user.
 * 3. If the session cannot be restored (refresh rejected, or the account is
 *    gone), it deletes the stale cookie and sends the user to `/sign-in`.
 * 4. If the backend is just not reachable (500/503), the cookie is kept: it is
 *    most likely still valid, so the user gets a retry instead of a logout.
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
  const tkStatus = (tk.error as { status?: number } | undefined)?.status;
  const meStatus = (me.error as { status?: number } | undefined)?.status;
  const sessionLost =
    (tk.isError && tkStatus === 401) ||
    (me.isError && (meStatus === 401 || meStatus === 404));
  // Any other refresh error (backend down, 500) says nothing about the cookie.
  const serverUnreachable = tk.isError && !sessionLost;

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

  if (serverUnreachable) {
    return (
      <div className="flex min-h-screen w-full flex-col items-center justify-center gap-4 bg-background">
        <p className="text-sm font-medium text-muted-foreground">
          Der Server ist gerade nicht erreichbar.
        </p>
        <Button
          onClick={() => tk.refetch()}
          disabled={tk.isFetching}
          className="hover:cursor-pointer"
        >
          {tk.isFetching ? "Verbinde …" : "Erneut versuchen"}
        </Button>
      </div>
    );
  }

  if (tk.isLoading || (tk.data && me.isLoading)) {
    return <FullPageLoader label="One moment, signing you in …" />;
  }

  return <>{children}</>;
};

export default AuthProvider;
