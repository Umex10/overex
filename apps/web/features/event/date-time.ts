/**
 * The event form has date and time as separate fields (date picker + time
 * input), the backend wants one `Instant`: these two convert between them.
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
