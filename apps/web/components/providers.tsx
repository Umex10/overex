"use client";

import { ThemeProvider } from "next-themes";
import StoreProvider from "@/redux/store-provider";
import { TooltipProvider } from "./ui/tooltip";

/**
 * Client providers shared by the whole app: the Redux store, the theme
 * provider (light/dark) and the tooltip provider.
 *
 * Note: AuthProvider is intentionally NOT here — it only wraps the protected
 * `(shell)` layout, so the public landing and auth pages don't pay for a token
 * lookup.
 */
export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <StoreProvider>
      <ThemeProvider
        attribute="class"
        defaultTheme="system"
        enableSystem
        disableTransitionOnChange
      >
        <TooltipProvider>{children}</TooltipProvider>
      </ThemeProvider>
    </StoreProvider>
  );
}
