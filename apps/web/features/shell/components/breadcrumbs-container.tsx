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

export default function BreadcrumbsContainer() {

  useBreadcrumbs();

  const { breadcrumbs } = useAppSelector((state) => state.routeContext);

  return (
      <Breadcrumb>
        <BreadcrumbList>
          {breadcrumbs.map((breadcrumb, index) => {
            // A crumb links to the path up to and including its
            // own segment: ["events", "5"] -> "/events", "/events/5".
            const href = "/" + breadcrumbs.slice(0, index + 1).join("/");
            const label = breadcrumb[0].toUpperCase() + breadcrumb.slice(1);
            const isLast = index === breadcrumbs.length - 1;

            return (
              <Fragment key={href}>
                <BreadcrumbItem>
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
