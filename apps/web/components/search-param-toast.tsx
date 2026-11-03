"use client";

import { Suspense, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { toast } from "sonner";

interface SearchParamToastProps {
  /** Query parameter to look at, e.g. `session` in `?session=expired`. */
  param: string;
  /** The toast only appears when the parameter has exactly this value. */
  value: string;
  message: string;
}

/**
 * Shows a toast when the URL carries `?param=value`, e.g. "session expired" on
 * `/sign-in` or "already signed in" on `/events`. A redirect sets the
 * parameter, so the target page can tell the user why they landed there.
 *
 * Renders nothing itself. Wrapped in `Suspense` because `useSearchParams` would
 * otherwise opt the whole page out of static rendering.
 */
export function SearchParamToast(props: SearchParamToastProps) {
  return (
    <Suspense fallback={null}>
      <ToastTrigger {...props} />
    </Suspense>
  );
}

function ToastTrigger({ param, value, message }: SearchParamToastProps) {
  const matches = useSearchParams().get(param) === value;

  useEffect(() => {
    // Fixed id: React runs effects twice in dev, and a toast with the same id
    // replaces the first one instead of stacking a second one on top.
    if (matches) toast.info(message, { id: `${param}=${value}` });
  }, [matches, message, param, value]);

  return null;
}
