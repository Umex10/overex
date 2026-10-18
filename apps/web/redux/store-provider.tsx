"use client";

import { useState } from "react";
import { Provider } from "react-redux";
import { makeStore } from "./store";

/**
 * Client-side wrapper that exposes the Redux store to React.
 *
 * The store is created once per mount (lazy `useState` initializer), i.e. once
 * per browser tab, and never shared between users on the server.
 */
export default function StoreProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [store] = useState(makeStore);
  return <Provider store={store}>{children}</Provider>;
}
