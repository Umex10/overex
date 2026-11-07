"use client";
import { setBreadcrumbs, setShowSearch } from "@/features/shell/route-context-slice";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import { usePathname } from "next/navigation";
import { useEffect } from "react";

import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import Link from "next/link";
import { Fragment } from "react";
import useBreadcrumbs from "../hooks/use-breadcrumbs";
import { useIsMobile } from "@/hooks/use-mobile";
import { ChevronLeftIcon } from "lucide-react";
import { cn } from "@/lib/utils";

/** Where the back arrow leads on a top-level page (no parent crumb). */
const FALLBACK_BACK_HREF = "/events";

export default function BreadcrumbsContainer() {

  useBreadcrumbs();

  const { breadcrumbs } = useAppSelector((state) => state.routeContext);
  const isMobile = useIsMobile();

  // On phones the crumbs sit on the page itself, not in the header bar, so
  // they get a pill with a back arrow to read as navigation. The arrow leads
  // one level up: /events/5 -> /events.
  const backHref =
    breadcrumbs.length > 1
      ? "/" + breadcrumbs.slice(0, -1).join("/")
      : FALLBACK_BACK_HREF;

  return (
      <Breadcrumb
        className={cn(
          isMobile &&
            "flex w-fit items-center gap-1.5 rounded-full border bg-muted/50 py-1.5 pr-3.5 pl-1.5 text-sm",
        )}
      >
        {isMobile && (
          <Link
            href={backHref}
            aria-label="Back"
            className="flex size-6 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <ChevronLeftIcon strokeWidth={2.5} className="size-4" />
          </Link>
        )}
        <BreadcrumbList>
          {breadcrumbs.map((breadcrumb, index) => {
            // A crumb links to the path up to and including its
            // own segment: ["events", "5"] -> "/events", "/events/5".
            const href = "/" + breadcrumbs.slice(0, index + 1).join("/");
            const label = breadcrumb[0].toUpperCase() + breadcrumb.slice(1);
            const isLast = index === breadcrumbs.length - 1;

            return (
              <Fragment key={href}>
                <BreadcrumbItem className="text-base">
                  {isLast ? (
                    <BreadcrumbPage>{label}</BreadcrumbPage>
                  ) : (
                    <BreadcrumbLink asChild>
                      <Link href={href}>{label}</Link>
                    </BreadcrumbLink>
                  )}
                </BreadcrumbItem>
                {!isLast && <BreadcrumbSeparator />}
              </Fragment>
            );
          })}
        </BreadcrumbList>
      </Breadcrumb>
  );
}
