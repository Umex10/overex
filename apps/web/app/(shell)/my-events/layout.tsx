import type { Metadata } from "next";

/**
 * Only here to set the tab title of `/my-events`: `page.tsx` is a client
 * component, and Next.js reads `metadata` from server components only.
 */
export const metadata: Metadata = {
  title: "Meine Events",
};

export default function MyEventsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
