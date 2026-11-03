import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { cn } from "@/lib/utils";
import { Providers } from "@/components/providers";
import { Toaster } from "@/components/ui/sonner";

const inter = Inter({ subsets: ["latin"], variable: "--font-inter" });

export const metadata: Metadata = {
  title: {
    // Pages without a title of their own (e.g. the landing page).
    default: "Overex — tickets for high-demand events",
    // Pages that set `title: "Events"` get "Events · Overex".
    template: "%s · Overex",
  },
  description:
    "Overex is a ticketing platform for high-demand events: Spring Boot backend + Next.js frontend.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={cn("h-full antialiased", inter.variable)}
      suppressHydrationWarning
    >
      <body className="min-h-full font-sans">
        <Providers>
          <Toaster position="top-right" closeButton />
          {children}
        </Providers>
      </body>
    </html>
  );
}
