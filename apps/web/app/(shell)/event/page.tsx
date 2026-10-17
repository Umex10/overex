"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useGetMeQuery } from "@/redux/api/apis/me";
import { Spinner } from "@/components/ui/loader";
// FUN GAG (optional): remove this import and the <Celebration/> usage below to
// turn the dashboard into a plain welcome screen.
import { Celebration } from "@/components/fun/Celebration";
import { SearchBar } from "@/components/search-bar";
import { useIsMobile } from "@/hooks/use-mobile";

/**
 * The post-login landing page.
 *
 * Reads the authenticated user from `GET /me` (proving the whole auth chain
 * works end to end) and greets them. The confetti celebration is an optional
 * "fun gag" — see components/fun/Celebration.tsx for how to remove it.
 */
export default function DashboardPage() {
  const { data: user, isLoading } = useGetMeQuery();

  const isMobile = useIsMobile();

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner className="size-8" />
      </div>
    );
  }

  return (
    <div className="w-full flex flex-col items-center p-2 gap-2">
     
     {isMobile && <SearchBar></SearchBar>}
      
     
    </div>
  );
}




