"use client";
import { useGetEventQuery } from "@/features/event/event-api";
import { setBreadcrumbs, setShowSearch } from "@/redux/route-context-slice";
import { AppDispatch, RootState } from "@/redux/store";
import { usePathname } from "next/navigation";
import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";

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
  const dispatch = useDispatch<AppDispatch>();

  const { breadcrumbs } = useSelector(
    (state: RootState) => state.routeContextSate,
  );

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
