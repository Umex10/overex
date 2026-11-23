"use client";

import { Controller, type Control } from "react-hook-form";

import { DatePicker } from "@/components/date-picker";
import { RequiredMark } from "@/components/required-mark";
import { Field, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import type {
  EventFormInput,
  EventFormValues,
} from "@/features/event/event-validation";

/**
 * A date picker and a time input side by side, both wired to the create-event
 * form. Used twice: for the event start and for the ticket sale start, so both
 * look and behave the same.
 *
 * `dateName` / `timeName` say which form fields the two inputs write to;
 * `idPrefix` keeps the ids unique when the component is on the page twice.
 */
export function DateTimeFields({
  control,
  dateName,
  timeName,
  idPrefix,
  dateLabel,
  timeLabel,
}: {
  control: Control<EventFormInput, unknown, EventFormValues>;
  dateName: "startDate" | "saleDate";
  timeName: "startTime" | "saleTime";
  idPrefix: string;
  dateLabel: string;
  timeLabel: string;
}) {
  const dateId = `${idPrefix}-date`;
  const timeId = `${idPrefix}-time`;

  return (
    <div className="grid w-full grid-cols-2 gap-3">
      <Controller
        name={dateName}
        control={control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid} className="gap-1">
            <FieldLabel htmlFor={dateId} className="text-base">
              {dateLabel} <RequiredMark />
            </FieldLabel>
            <DatePicker
              id={dateId}
              value={field.value}
              onChange={field.onChange}
              invalid={fieldState.invalid}
              disabled={{ before: new Date() }}
            />
            <FieldError errors={[fieldState.error]} />
          </Field>
        )}
      />

      <Controller
        name={timeName}
        control={control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid} className="gap-1">
            <FieldLabel htmlFor={timeId} className="text-base">
              {timeLabel} <RequiredMark />
            </FieldLabel>
            <Input
              {...field}
              id={timeId}
              type="time"
              step={60}
              required
              aria-invalid={fieldState.invalid}
            />
            <FieldError errors={[fieldState.error]} />
          </Field>
        )}
      />
    </div>
  );
}
