"use client";

import { useState } from "react";
import { format } from "date-fns";
import { de } from "date-fns/locale";
import { CalendarIcon } from "lucide-react";
import type { Matcher } from "react-day-picker";

import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";

/**
 * Date picker built from shadcn parts: a button styled like an input opens a
 * popover with the calendar. The popover closes again once a day is picked.
 *
 * Controlled: the caller owns the value (`value` / `onChange`), so the
 * component works with `useState` as well as with react-hook-form.
 */
export function DatePicker({
  value,
  onChange,
  id,
  placeholder = "tt.mm.jjjj",
  disabled,
  invalid,
  className,
}: {
  value: Date | undefined;
  onChange: (date: Date | undefined) => void;
  /** Lets a `FieldLabel htmlFor` point at the trigger button. */
  id?: string;
  placeholder?: string;
  /** Days that can't be picked, e.g. `{ before: new Date() }` for future-only. */
  disabled?: Matcher | Matcher[];
  /** Shows the error state, e.g. when validation failed. */
  invalid?: boolean;
  className?: string;
}) {
  const [open, setOpen] = useState(false);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          id={id}
          type="button"
          variant="outline"
          aria-invalid={invalid}
          className={cn(
            "h-11 w-full justify-between px-3.5 text-base font-normal md:h-10 md:text-sm",
            className,
          )}
        >
          {value ? (
            format(value, "dd.MM.yyyy")
          ) : (
            <span className="text-muted-foreground">{placeholder}</span>
          )}
          <CalendarIcon className="text-muted-foreground" />
        </Button>
      </PopoverTrigger>

      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          mode="single"
          selected={value}
          defaultMonth={value}
          onSelect={(date) => {
            onChange(date);
            setOpen(false);
          }}
          disabled={disabled}
          locale={de}
          autoFocus
        />
      </PopoverContent>
    </Popover>
  );
}
