"use client" 

import { setBreadcrumbs, setShowSearch } from "@/features/shell/route-context-slice";
import { useAppDispatch } from "@/redux/hooks";
import { usePathname } from "next/navigation";
import { useEffect } from "react";

export default function useBreadcrumbs() {
  const dispatch = useAppDispatch();

  const path = usePathname();
  const segments = path.split("/").filter(Boolean); // ["events", "5"]

  useEffect(() => {
    dispatch(setBreadcrumbs(segments));
  }, [dispatch, path]);
}
