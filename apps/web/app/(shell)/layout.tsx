"use client";

import AuthProvider from "@/components/AuthProvider";
import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";
import { useIsMobile } from "@/hooks/use-mobile";
import { SidebarLogo } from "@/components/sidebar-logo";
import SignInButton from "@/components/sign-in-button";
import { SearchBar } from "@/components/search-bar";

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
  const isMobile = useIsMobile();

  return (
    <AuthProvider>
      <SidebarProvider>
        <AppSidebar />
        {/* flex-1: takes the space right of the sidebar; min-w-0 lets wide
              content shrink instead of pushing the layout wider. */}
        <div className="min-w-0 flex-1 flex flex-row overflow-x-hidden">
          <div className="w-full flex flex-col">
            <header
              className="w-full flex flex-row items-center justify-between gap-4
            border-b py-2 px-4"
            >
              {/* Trigger and title in one row, so both are centred on the
                  same line as the search field. */}
              <div className="flex flex-row shrink-0 items-center gap-2">
                {/* size-auto: the button shrinks to the icon instead of the
                    fixed size-7 square, so there is no empty space around it. */}
                <SidebarTrigger className="size-auto hover:cursor-pointer" />

                {isMobile ? <SidebarLogo></SidebarLogo> 
                : <h2>Event</h2>}
              </div>

              {isMobile ? (
                <SignInButton></SignInButton>
              ) : (
                // Full width by default; from md on capped at 20rem.
                <SearchBar className="md:max-w-80" />
              )}
            </header>
            <main className="w-full flex justify-center items-center p-2">
              {children}
            </main>
          </div>
        </div>
      </SidebarProvider>
    </AuthProvider>
  );
}
