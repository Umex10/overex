"use client";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { deleteRefreshCookie } from "@/features/auth/auth-actions";

/**
 * Signs the user out by deleting the refresh-token cookie (the backend is
 * stateless, so no server round-trip is required) and sending them to
 * `/sign-in`. Styled like `SignInButton`.
 */
export function SignOutButton({ className }: { className?: string }) {
  const handleSignOut = async () => {
    await deleteRefreshCookie();
    // Full reload so the RTK Query cache (incl. the access token) is cleared.
    window.location.href = "/sign-in";
  };

  return (
    <Button
      data-testid="header-sign-out-button"
      onClick={handleSignOut}
      className={cn(
        "h-9 rounded-lg bg-white text-zinc-950 transition-all duration-150 hover:cursor-pointer hover:bg-zinc-200 hover:shadow-md active:scale-[0.98]",
        className,
      )}
    >
      Abmelden
    </Button>
  );
}
