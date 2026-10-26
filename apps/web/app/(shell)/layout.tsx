"use client";

import { cn } from "@/lib/utils";

import AuthProvider from "@/features/auth/components/auth-provider";
import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";
import { useIsMobile } from "@/hooks/use-mobile";
import { SidebarLogo } from "@/components/sidebar-logo";
import SignInButton from "@/features/auth/components/sign-in-button";
import { SearchBar } from "@/components/search-bar";
import { Separator } from "@/components/ui/separator";
import { useAppSelector } from "@/redux/hooks";

import BreadcrumbsContainer from "@/components/BreadcrumbsContainer";
import { useGetMeQuery } from "@/features/user/user-api";

/**
 * Layout for every protected route under `(shell)`.
 *
 * `AuthProvider` gates rendering until an access token is resolved (and the
 * current user prefetched), so child pages can assume they are authenticated.
 * Around the page sit the sidebar and a top bar with the sidebar toggle and,
 * on desktop, the page title and the search field; on phones the logo and the
 * sign-in button take their place.
 */
export default function ShellLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const isMobile = useIsMobile();
  const { showSearch } = useAppSelector((state) => state.routeContext);

  const { data: me, isLoading: isLoadingMe } = useGetMeQuery();

  return (
    <AuthProvider>
      <SidebarProvider>
        <AppSidebar />
        {/* flex-1: takes the space right of the sidebar; min-w-0 lets wide
              content shrink instead of pushing the layout wider. */}
        <div className="min-w-0 flex-1 flex flex-row overflow-x-hidden">
          <div className="w-full flex flex-col">
            <header
              className={cn(
                "w-full items-center border-b py-2 px-4",
                // Phones: three columns, the outer two always equally wide, so
                // the logo in the middle column sits exactly in the centre.
                isMobile
                  ? "grid grid-cols-[1fr_auto_1fr]"
                  : "flex flex-row justify-between gap-4",
              )}
            >
              {isMobile ? (
                <>
                  <SidebarTrigger className="size-auto justify-self-start hover:cursor-pointer" />
                  <SidebarLogo />
                  {!me && (
                    <SignInButton className="justify-self-end"></SignInButton>
                  )}
                </>
              ) : (
                <>
                  {/* Trigger and title in one row, so both are centred on the
                      same line as the search field. */}
                  <div className="flex flex-row shrink-0 items-center gap-2">
                    {/* size-auto: the button shrinks to the icon instead of the
                        fixed size-7 square, so there is no empty space around it. */}
                    <SidebarTrigger className="size-auto hover:cursor-pointer" />

                    {/* Vertical line between trigger and title. Fixed height
                        instead of the default self-stretch, so it stays
                        shorter than the text next to it. */}
                    <Separator
                      orientation="vertical"
                      className="data-vertical:h-4 data-vertical:self-center"
                    />
                    <BreadcrumbsContainer></BreadcrumbsContainer>
                  </div>

                  {showSearch && (
                    // Full width by default; from md on capped at 20rem.
                    <SearchBar className="md:max-w-80" />
                  )}
                </>
              )}
            </header>
            <main className="w-full flex justify-center items-center p-4">
              {children}
            </main>
          </div>
        </div>
      </SidebarProvider>
    </AuthProvider>
  );
}
