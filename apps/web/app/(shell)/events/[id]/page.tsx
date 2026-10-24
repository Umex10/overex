"use client";
import { useGetEventQuery } from "@/features/event/event-api";
import { setBreadcrumbs, setShowSearch } from "@/redux/route-context-slice";
import { AppDispatch, RootState } from "@/redux/store";
import { useParams, usePathname } from "next/navigation";
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
import { useIsMobile } from "@/hooks/use-mobile";

export default function EventDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { data: event, isLoading: isLoadingEvent } = useGetEventQuery(id);
  const path = usePathname();
  const segments = path.split("/").filter(Boolean); // ["events", "5"]
  const dispatch = useDispatch<AppDispatch>();

  const isMobile = useIsMobile();
  const { breadcrumbs } = useSelector(
    (state: RootState) => state.routeContextSate,
  );

  useEffect(() => {
    dispatch(setBreadcrumbs(segments));
    dispatch(setShowSearch(false));
  }, [dispatch]);

  return (
    <div className="w-full flex flex-row justify-start">
      {isMobile && (
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
      )}
    </div>
  );
}
