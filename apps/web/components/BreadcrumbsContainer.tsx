"use client";
import { useGetEventQuery } from "@/features/event/event-api";
import { setBreadcrumbs, setShowSearch } from "@/redux/route-context-slice";
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

export default function BreadcrumbsContainer() {
  const path = usePathname();
  const segments = path.split("/").filter(Boolean); // ["events", "5"]
  const dispatch = useAppDispatch();

  const { breadcrumbs } = useAppSelector((state) => state.routeContext);

  useEffect(() => {
    dispatch(setBreadcrumbs(segments));
  }, [dispatch]);

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
