"use client";

import Link from "next/link";
import { TicketIcon } from "@phosphor-icons/react";
import { useSidebar } from "@/components/ui/sidebar";
import { cn } from "@/lib/utils";

/**
 * Overex logo for the sidebar header.
 *
 * Expanded: links to the start page. Collapsed (icon rail): opens the sidebar
 * instead of navigating. Extra props (e.g. the classes `SidebarMenuButton
 * asChild` passes down) are forwarded to the link.
 *
 * The link brings its own row layout, so the logo looks the same inside the
 * sidebar and on its own (e.g. in the mobile header); classes from outside are
 * merged on top via `cn`.
 */
export function SidebarLogo({
  className,
  onClick,
  ...props
}: Omit<React.ComponentProps<typeof Link>, "href">) {
  const { state, toggleSidebar } = useSidebar();
  const collapsed = state === "collapsed";

  return (
    <Link
      href="/event"
      aria-label={collapsed ? "Open sidebar" : "Overex"}
      className={cn("flex items-center gap-1", className)}
      {...props}
      onClick={(e) => {
        onClick?.(e);
        if (collapsed) {
          e.preventDefault();
          toggleSidebar();
        }
      }}
    >
      <span className="flex aspect-square size-8 shrink-0 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
        <TicketIcon weight="fill" className="size-6! -rotate-12" />
      </span>
      <span className="text-base font-semibold tracking-tight">Overex</span>
    </Link>
  );
}
