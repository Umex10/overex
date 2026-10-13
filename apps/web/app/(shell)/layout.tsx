import Link from "next/link";
import AuthProvider from "@/components/AuthProvider";
import { ThemeToggle } from "@/components/theme-toggle";
import { SignOutButton } from "@/components/SignOutButton";

/**
 * Layout for every protected route under `(shell)`.
 *
 * `AuthProvider` gates rendering until an access token is resolved (and the
 * current user prefetched), so child pages can assume they are authenticated.
 * A minimal top bar provides the theme toggle and sign-out.
 */
export default function ShellLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthProvider>
      <div className="flex min-h-screen flex-col">
        <header className="sticky top-0 z-40 border-b border-border bg-background/80 backdrop-blur">
          <div className="mx-auto flex h-16 w-full max-w-5xl items-center justify-between px-6">
            {/* Goes to the root URL on purpose. If you're still signed in,
                proxy.ts will bounce you right back to /dashboard with the
                "already signed in" note — demonstrating the redirect flow. */}
            <Link href="/" className="flex items-center gap-2 font-semibold">
              <span className="text-xl">🔐</span>
              <span>Overex</span>
            </Link>
            <div className="flex items-center gap-2">
              <ThemeToggle />
              <SignOutButton />
            </div>
          </div>
        </header>

        <main className="mx-auto w-full max-w-5xl flex-1 px-6 py-10">
          {children}
        </main>
      </div>
    </AuthProvider>
  );
}
