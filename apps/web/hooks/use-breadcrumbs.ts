import { setBreadcrumbs, setShowSearch } from "@/redux/route-context-slice";
import { AppDispatch } from "@/redux/store";
import { usePathname } from "next/navigation";
import { useEffect } from "react";
import { useDispatch } from "react-redux";

export default function useBreadcrumbs() {
  const dispatch = useDispatch<AppDispatch>();

  const path = usePathname();
  const segments = path.split("/").filter(Boolean); // ["events", "5"]

  useEffect(() => {
    dispatch(setBreadcrumbs(segments));
  }, [dispatch]);
}
