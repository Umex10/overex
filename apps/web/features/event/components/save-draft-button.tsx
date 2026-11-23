import { Button } from "@/components/ui/button";

/**
 * "Entwurf speichern" and "Veröffentlichen" of the create-event page. Shown
 * twice: next to the title and, on phones, in the sticky bar at the bottom.
 *
 * Plain `type="button"`s: each one calls its own handler (validate, then send
 * with DRAFT or PUBLISHED), so it doesn't matter that they sit outside <form>.
 */
export function SaveDraftButton({
  onSaveDraft,
  disabled,
}: {
  onSaveDraft: () => void;
  /** While a request is running, so nobody creates the event twice. */
  disabled?: boolean;
}) {
  return (
    <Button
      type="button"
      variant="outline"
      onClick={onSaveDraft}
      disabled={disabled}
      className="rounded-lg p-6 text-base"
    >
      Entwurf speichern
    </Button>
  );
}
