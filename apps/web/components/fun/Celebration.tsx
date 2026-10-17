"use client";

/* ───────────────────────────────────────────────────────────────────────────
 * FUN GAG — a celebratory confetti burst + a rotating silly subtitle.
 *
 * This whole folder (components/fun/) is the "just for fun" layer. To remove
 * the gag completely:
 *   1. delete this file (and the components/fun/ folder),
 *   2. remove the <Celebration /> usage in app/(shell)/event/page.tsx,
 *   3. (optional) delete the "FUN GAG" block in app/globals.css.
 * Nothing else in the app depends on it.
 * ─────────────────────────────────────────────────────────────────────────── */

import { useState } from "react";
import { useHydrated } from "@/lib/use-hydrated";

const CONFETTI_COLORS = [
  "#7c3aed",
  "#8b5cf6",
  "#a78bfa",
  "#f59e0b",
  "#22c55e",
  "#ec4899",
];

/** A few interchangeable, deliberately goofy congratulation lines. */
const SUBTITLES = [
  "You did it! 🎉",
  "Auth works. You're officially a wizard. 🪄",
  "Token valid, vibes priceless. ✨",
  "Logged in like a pro. 😎",
  "That's it — auth is done, go grab a coffee. ☕",
];

/** Deterministic pseudo-random so confetti looks varied without Math.random in render. */
function pseudoRandom(seed: number) {
  const x = Math.sin(seed * 99.13) * 10000;
  return x - Math.floor(x);
}

export function Celebration() {
  // true in the browser, false during server rendering: confetti and the random
  // line are client-only, so server and client HTML always match.
  const mounted = useHydrated();
  // Picked once per mount, not on every render.
  const [randomLine] = useState(
    () => SUBTITLES[Math.floor(Math.random() * SUBTITLES.length)],
  );
  const subtitle = mounted ? randomLine : SUBTITLES[0];

  return (
    <div className="ak-pop-in flex flex-col items-center gap-3 text-center">
      <div className="text-6xl" aria-hidden>
        🎉
      </div>
      <p className="text-base font-medium text-muted-foreground">{subtitle}</p>

      {/* Confetti rain (fixed, pointer-events-none so it never blocks clicks) */}
      {mounted && (
        <div className="pointer-events-none fixed inset-0 z-50 overflow-hidden" aria-hidden>
          {Array.from({ length: 80 }).map((_, i) => {
            const left = pseudoRandom(i + 1) * 100;
            const delay = pseudoRandom(i + 2) * 0.8;
            const duration = 2.6 + pseudoRandom(i + 3) * 1.8;
            const color = CONFETTI_COLORS[i % CONFETTI_COLORS.length];
            return (
              <span
                key={i}
                className="ak-confetti-piece"
                style={{
                  left: `${left}vw`,
                  background: color,
                  animationDelay: `${delay}s`,
                  animationDuration: `${duration}s`,
                }}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}
