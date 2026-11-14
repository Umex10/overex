import * as z from "zod";

/**
 * Validation rules for the create-event form. They mirror the backend's
 * `CreateEventDto`, so the user sees the same rule before the request is sent.
 *
 * Date and time are separate fields in the form (date picker + time input) and
 * only become one `Instant` on submit, see {@link combineDateAndTime}.
 */

/** Joins the picked day and an "HH:mm" string into one local `Date`. */
export function combineDateAndTime(date: Date, time: string): Date {
  const [hours, minutes] = time.split(":").map(Number);
  const combined = new Date(date);
  combined.setHours(hours, minutes, 0, 0);
  return combined;
}

/**
 * Opposite of {@link combineDateAndTime}: splits a point in time (e.g. an
 * ISO `Instant` from the backend) into the local day and an "HH:mm" string,
 * so it can fill the date picker and the time input of the form.
 */
export function uncombineDateAndTime(value: Date | string): {
  date: Date;
  time: string;
} {
  const source = new Date(value);

  const date = new Date(source);
  date.setHours(0, 0, 0, 0);

  const hours = String(source.getHours()).padStart(2, "0");
  const minutes = String(source.getMinutes()).padStart(2, "0");

  return { date, time: `${hours}:${minutes}` };
}

export const eventSchema = z
  .object({
    title: z
      .string()
      .trim()
      .min(1, "Title is required.")
      .max(120, "Title must be at most 120 characters."),
    categoryId: z.string().min(1, "Please choose a category."),
    description: z.string().trim().min(1, "Description is required."),
    imageUrl: z
      .string()
      .trim()
      .min(1, "Image URL is required.")
      .max(500, "Image URL must be at most 500 characters.")
      .pipe(z.url("Please enter a valid image URL.")),

    startDate: z.date({ error: "Please choose a date." }),
    startTime: z.string().min(1, "Please choose a start time."),

    venue: z
      .string()
      .trim()
      .min(1, "Venue is required.")
      .max(120, "Venue must be at most 120 characters."),
    city: z
      .string()
      .trim()
      .min(1, "City is required.")
      .max(80, "City must be at most 80 characters."),

    saleDate: z.date({ error: "Please choose a date." }),
    saleTime: z.string().min(1, "Please choose a time."),

    // Same values as the backend's `Status` enum (see EventStatus).
    status: z.enum(["DRAFT", "PUBLISHED", "CANCELED"], {
      error: "Please choose a status.",
    }),
  })
  // Rules that need more than one field. They only run once every single
  // field above is valid, so date and time are guaranteed to be set here.
  .superRefine((values, ctx) => {
    const startsAt = combineDateAndTime(values.startDate, values.startTime);
    const saleStartsAt = combineDateAndTime(values.saleDate, values.saleTime);

    if (startsAt <= new Date()) {
      ctx.addIssue({
        code: "custom",
        message: "The event must start in the future.",
        path: ["startTime"],
      });
    }

    if (saleStartsAt >= startsAt) {
      ctx.addIssue({
        code: "custom",
        message: "Ticket sale must start before the event.",
        path: ["saleDate"],
      });
    }
  })
  // The form needs date and time as separate fields (date picker + time
  // input), the backend wants one Instant each. After validation, both pairs
  // become startsAt / saleStartsAt, so onSubmit gets exactly CreateEventDto.
  .transform(({ startDate, startTime, saleDate, saleTime, ...rest }) => ({
    ...rest,
    startsAt: combineDateAndTime(startDate, startTime).toISOString(),
    saleStartsAt: combineDateAndTime(saleDate, saleTime).toISOString(),
  }));

/** What the form fields hold: date and time still separate. */
export type EventFormInput = z.input<typeof eventSchema>;
/** What `onSubmit` receives: validated, with startsAt / saleStartsAt as ISO strings. */
export type EventFormValues = z.output<typeof eventSchema>;
