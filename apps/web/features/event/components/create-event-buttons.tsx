import { Button } from "@/components/ui/button";

/**
 * "Entwurf speichern" and "Veröffentlichen" of the create-event page. Shown
 * twice: next to the title and, on phones, in the sticky bar at the bottom.
 * Both sit outside the <form>, so they point at it via `form="…"`.
 */
export function CreateEventButtons() {
  return (
    <>
      <Button type="submit" className="rounded-lg text-base p-6 hover:cursor-pointer">
        Entwurf speichern
      </Button>
      <Button
        form="create-event-form"
        type="submit"
        className="rounded-lg text-base p-6 hover:cursor-pointer"
      >
        Veröffentlichen
      </Button>
    </>
  );
}
