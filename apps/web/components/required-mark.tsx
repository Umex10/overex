/**
 * Red asterisk behind the label of a required field. Hidden from screen
 * readers: the input's `required` / `aria-required` already says "required".
 */
export function RequiredMark() {
  return (
    <span aria-hidden className="text-destructive">
      *
    </span>
  );
}
