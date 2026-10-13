"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useGetMeQuery } from "@/redux/api/apis/me";
import { Spinner } from "@/components/ui/loader";
// FUN GAG (optional): remove this import and the <Celebration/> usage below to
// turn the dashboard into a plain welcome screen.
import { Celebration } from "@/components/fun/Celebration";

/**
 * The post-login landing page.
 *
 * Reads the authenticated user from `GET /me` (proving the whole auth chain
 * works end to end) and greets them. The confetti celebration is an optional
 * "fun gag" — see components/fun/Celebration.tsx for how to remove it.
 */
export default function DashboardPage() {
  const { data: user, isLoading } = useGetMeQuery();

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner className="size-8" />
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center gap-10 py-10">
      {/* Shown only when proxy.ts bounced an already-signed-in visitor here. */}
      <Suspense fallback={null}>
        <RedirectNote />
      </Suspense>

      {/* FUN GAG (optional) — confetti + a goofy subtitle. */}
      <Celebration />

      <div className="flex flex-col items-center gap-2 text-center">
        <h1
          data-testid="dashboard-welcome"
          className="text-3xl font-extrabold tracking-tight sm:text-4xl"
        >
          You made it{user?.name ? `, ${user.name}` : ""}! 🚀
        </h1>
        <p className="max-w-md text-sm leading-relaxed text-muted-foreground">
          You’re logged in. That means sign-up/sign-in, the HTTP-only refresh
          cookie, the access token and the protected route{" "}
          <code className="rounded bg-muted px-1 py-0.5">GET /me</code> all
          worked.
        </p>
      </div>

      {/* The authenticated user, straight from the protected /me endpoint. */}
      {user && (
        <div
          data-testid="dashboard-account"
          className="w-full max-w-md rounded-xl border border-border bg-card p-6"
        >
          <h2 className="mb-4 text-sm font-semibold text-muted-foreground">
            Your account (via <code>GET /me</code>)
          </h2>
          <dl className="flex flex-col gap-3 text-sm">
            <Row label="Name" value={user.name} />
            <Row label="Email" value={user.email} />
            <Row label="Phone" value={user.phone} />
            <Row
              label="Role"
              value={
                <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                  {user.role}
                </span>
              }
            />
          </dl>
        </div>
      )}

      <div className="w-full max-w-md rounded-xl border border-dashed border-border p-5 text-sm text-muted-foreground">
        <p className="font-semibold text-foreground">What’s next?</p>
        <ul className="mt-2 flex list-disc flex-col gap-1.5 pl-5">
          <li>Build your features under <code>app/(shell)/</code> — they’re protected automatically.</li>
          <li>Add new API calls as RTK Query endpoints next to <code>me.ts</code>.</li>
          <li>Roles/logic live in the backend under <code>com.overex.backend</code>.</li>
        </ul>
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}

/**
 * A small banner shown when `proxy.ts` redirected an already-authenticated
 * visitor here (from `/`, `/sign-in` or `/sign-up`). It surfaces the convenience
 * redirect so the user understands they didn't have to sign in again — the
 * refresh cookie was still valid. Reads the `?redirected=1` flag the middleware
 * appends.
 */
function RedirectNote() {
  const wasRedirected = useSearchParams().get("redirected") === "1";
  if (!wasRedirected) return null;

  return (
    <div
      data-testid="dashboard-redirect-note"
      className="w-full max-w-md rounded-xl border border-primary/30 bg-primary/10 px-4 py-3 text-sm text-foreground"
    >
      <p className="font-semibold text-primary">👋 Welcome back — you’re already signed in.</p>
      <p className="mt-1 text-muted-foreground">
        Your refresh cookie was still valid, so{" "}
        <code className="rounded bg-muted px-1 py-0.5">proxy.ts</code> sent you
        straight here instead of showing the landing or sign-in page again.
      </p>
    </div>
  );
}
