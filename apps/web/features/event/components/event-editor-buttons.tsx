import { Controller } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Field, FieldError, FieldLabel } from "@/components/ui/field";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { RequiredMark } from "@/components/required-mark";
import type { EventEditorMode } from "@/components/EventEditor";
import type { EventEditorForm } from "@/features/event/hooks/use-event-editor";
import { StatusOptionMap } from "../event-types";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";

/**
 * Status select and the submit button of the event editor. Shown twice: next
 * to the title and, on phones, in the sticky bar at the bottom.
 *
 * Plain `type="button"`s: each one calls its own handler (validate, then
 * send), so it doesn't matter that they sit outside <form>.
 */
export function EventEditorButtons({
  form,
  mode,
  statusOptionsMap,
  onPublish,
  onEdit,
}: {
  /** The form of {@link useEventEditor}; the select writes `status` into it. */
  form: EventEditorForm;
  mode: EventEditorMode;
  statusOptionsMap: StatusOptionMap;
  /** Validates and creates the event (CREATE mode). */
  onPublish: () => void;
  /** Validates and saves the changes (EDIT mode). */
  onEdit: () => void;
}) {
  return (
    <>
      <Controller
        name="status"
        control={form.control}
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid} className="gap-1">
            <FieldLabel htmlFor="status" className="text-base">
              Status <RequiredMark />
            </FieldLabel>
            <Select
              name={field.name}
              value={field.value}
              onValueChange={field.onChange}
            >
              <SelectTrigger
                id="status"
                onBlur={field.onBlur}
                aria-invalid={fieldState.invalid}
                className="w-full"
              >
                <SelectValue placeholder="Choose a Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  {Object.entries(statusOptionsMap).map(([status, option]) =>
                    // Only a blocked status has a reason, so only it gets a tooltip.
                    option.why ? (
                      <Tooltip key={status}>
                        {/* asChild: the trigger is this <div>, not an extra
                            <button> that would break arrow keys and Enter.
                            A disabled item has pointer-events: none, so the
                            mouse lands on the <div> and the tooltip still opens.
                            That's also why the not-allowed cursor sits here:
                            on the item itself it would never show. */}
                        <TooltipTrigger asChild>
                          <div className="cursor-not-allowed">
                            <SelectItem value={status} disabled={option.disabled}>
                              {status}
                            </SelectItem>
                          </div>
                        </TooltipTrigger>
                        <TooltipContent side="right">{option.why}</TooltipContent>
                      </Tooltip>
                    ) : (
                      <SelectItem
                        key={status}
                        value={status}
                        disabled={option.disabled}
                      >
                        {status}
                      </SelectItem>
                    ),
                  )}
                </SelectGroup>
              </SelectContent>
            </Select>
            <FieldError errors={[fieldState.error]} />
          </Field>
        )}
      />

      {mode === "CREATE" ? (
        <Button
          type="button"
          onClick={onPublish}
          disabled={!form.formState.isDirty || form.formState.isSubmitting}
          className="rounded-lg p-6 text-base"
        >
          Create
        </Button>
      ) : (
        <Button
          type="button"
          onClick={onEdit}
          disabled={!form.formState.isDirty || form.formState.isSubmitting}
          className="rounded-lg p-6 text-base"
        >
          Save
        </Button>
      )}
    </>
  );
}
