"use client";

import Countdown, { zeroPad, type CountdownRenderProps } from "react-countdown";
import { cn } from "@/lib/utils";

/**
 * Live countdown to `date` as four tiles (days, hours, minutes, seconds).
 *
 * `react-countdown` re-renders every second and hands the remaining time to
 * the renderer below; once `date` is reached it shows `completedText` instead.
 */
export function CountdownCards({
  date,
  completedText = "Jetzt verfügbar",
  className,
}: {
  date: Date;
  completedText?: string;
  className?: string;
}) {
  return (
    <Countdown
      date={date}
      renderer={({ days, hours, minutes, seconds, completed }: CountdownRenderProps) =>
        completed ? (
          <p className={cn("text-sm font-semibold", className)}>{completedText}</p>
        ) : (
          <div className={cn("grid w-full grid-cols-4 gap-2", className)}>
            <CountdownCard value={days} label="Tage" />
            <CountdownCard value={hours} label="Std" />
            <CountdownCard value={minutes} label="Min" />
            <CountdownCard value={seconds} label="Sek" />
          </div>
        )
      }
    />
  );
}

function CountdownCard({ value, label }: { value: number; label: string }) {
  return (
    <div className="flex flex-col items-center rounded-lg border bg-muted/40 px-2 py-2">
      {/* tabular-nums: all digits have the same width, so the tile doesn't
          jitter while the seconds change. */}
      <span className="text-2xl font-bold tabular-nums">{zeroPad(value)}</span>
      <span className="text-[10px] tracking-wide text-muted-foreground uppercase">
        {label}
      </span>
    </div>
  );
}
