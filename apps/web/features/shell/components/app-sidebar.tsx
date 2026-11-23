"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupAction,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";
import type { LucideIcon } from "lucide-react";
import {
  CalendarDaysIcon,
  ZapIcon,
  CirclePlusIcon,
  TicketIcon,
  CalendarRangeIcon,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { SidebarLogo } from "@/features/shell/components/sidebar-logo";

import SignInButton from "@/features/auth/components/sign-in-button";
import { useGetMeQuery } from "@/features/user/user-api";
import { SignOutButton } from "@/features/auth/components/sign-out-button";

interface NavItem {
  icon: LucideIcon;
  title: string;
  href: string;
}

const NAV_ITEMS: NavItem[] = [
  {
    icon: CalendarDaysIcon,
    title: "Events",
    href: "/events",
  },
  {
    icon: TicketIcon,
    title: "My Tickets",
    href: "/my-tickets",
  },
  {
    icon: CalendarRangeIcon,
    title: "Meine Events",
    href: "/my-events",
  },
  {
    icon: CirclePlusIcon,
    title: "Event erstellen",
    href: "/create-event",
  },
];

/**
 * Grey sidebar with the main navigation. The entry of the current page is
 * highlighted in the brand colour (`--sidebar-primary`), hover uses a slightly darker
 * grey (`--sidebar-accent`); both colours live in app/globals.css.
 *
 * Closing it collapses it to an icon rail (`collapsible="icon"`): only the logo
 * and the nav icons stay visible, each nav icon shows its title as a tooltip,
 * and a click on the logo opens the sidebar again.
 */
export function AppSidebar() {
  const pathname = usePathname();
  const { state, isMobile, setOpenMobile } = useSidebar();
  const collapsed = state === "collapsed";

  const { data: me } = useGetMeQuery();

  return (
    <Sidebar
      collapsible="icon"
      // The sidebar is position: fixed, so it ignores the centred wrapper and
      // would stick to the window edge. Move it to the wrapper's left edge:
      // half of the free space, never below 0. For fixed elements 100% is the
      // window width without the scrollbar.
      className="data-[side=left]:left-[max(0px,calc((100%-var(--app-max-width))/2))]"
    >
      <SidebarHeader className="group-data-[state=expanded]:px-0">
        <SidebarMenu>
          <SidebarMenuItem onClick={() => isMobile && setOpenMobile(false)}>
            <SidebarMenuButton
              asChild
              size="lg"
              tooltip={collapsed ? "Open sidebar" : undefined}
              className="hover:bg-transparent active:bg-transparent"
            >
              <SidebarLogo />
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel className="text-sm">Application</SidebarGroupLabel>
          <SidebarGroupAction>
            <span className="sr-only">Add Project</span>
          </SidebarGroupAction>
          <SidebarGroupContent>
            <SidebarMenu className="gap-1.5">
              {NAV_ITEMS.map((n) => (
                <SidebarMenuItem
                  key={n.title}
                  // On phones the sidebar is an overlay sheet: close it after
                  // navigating so the new page is visible.
                  onClick={() => isMobile && setOpenMobile(false)}
                >
                  <SidebarMenuButton
                    asChild
                    isActive={pathname.startsWith(n.href)}
                    tooltip={n.title}
                    className="h-12 gap-3 rounded-lg px-3 text-base transition-colors duration-150 md:h-11 [&_svg]:size-5 data-[active=false]:bg-transparent data-[active=false]:font-normal data-[active=false]:hover:bg-sidebar-accent data-[active=true]:bg-sidebar-primary data-[active=true]:text-sidebar-primary-foreground data-[active=true]:hover:bg-sidebar-primary/90 data-[active=true]:hover:text-sidebar-primary-foreground"
                  >
                    <Link href={n.href}>
                      <n.icon />
                      <span>{n.title}</span>
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
        <SidebarGroup />
      </SidebarContent>
      {/* Hidden in the icon rail: the card doesn't fit into 3rem. */}
      <SidebarFooter className="group-data-[collapsible=icon]:hidden">
        {!me ? (
          <div className="relative overflow-hidden rounded-xl border border-white/10 bg-zinc-950 p-4 text-zinc-100 shadow-lg">
            {/* Soft glow in the brand colour in the top right corner. */}
            <div
              aria-hidden
              className="pointer-events-none absolute -top-10 -right-10 size-28 rounded-full bg-primary/40 blur-2xl"
            />

            <div className="relative flex flex-col gap-3">
              <div className="flex items-center gap-2">
                <span className="flex size-7 items-center justify-center rounded-lg bg-white/10">
                  <ZapIcon
                    fill="currentColor"
                    className="size-4 text-amber-300"
                  />
                </span>
                <p className="text-sm font-semibold">Bereit für den Drop?</p>
              </div>

              <p className="text-sm leading-relaxed text-zinc-400">
                Melde dich vor dem Drop an, dann geht der Kauf schneller.
              </p>

              <div className="flex flex-col gap-2">
                <SignInButton></SignInButton>

                <Button
                  asChild
                  variant="outline"
                  className="h-10 rounded-lg border-white/20 bg-transparent text-zinc-100 transition-all duration-150 hover:border-white/40 hover:bg-white/10 hover:text-white active:scale-[0.98] dark:bg-transparent"
                >
                  <Link href="/sign-up">Konto erstellen</Link>
                </Button>
              </div>
            </div>
          </div>
        ) : (
          <SignOutButton></SignOutButton>
        )}
      </SidebarFooter>
    </Sidebar>
  );
}
