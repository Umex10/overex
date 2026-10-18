import type { Metadata } from "next";

/**
 * Only here to set the tab title of `/event`: `page.tsx` is a client
 * component, and Next.js reads `metadata` from server components only.
 * The root layout's title template turns "Events" into "Events · Overex".
 */
export const metadata: Metadata = {
  title: "Events",
};

export default function EventLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
