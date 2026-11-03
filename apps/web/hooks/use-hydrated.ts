import { useSyncExternalStore } from "react";

/** No-op subscribe: the answer never changes after the first client render. */
const subscribe = () => () => {};

/**
 * `false` in the server-rendered HTML, `true` as soon as React has taken over
 * the page in the browser.
 *
 * The auth forms keep their submit button disabled until then. Clicking it
 * earlier would let the browser submit the plain HTML form itself, bypassing
 * validation and the server action.
 */
export function useHydrated() {
  return useSyncExternalStore(subscribe, () => true, () => false);
}
