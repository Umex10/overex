"use client";

import { Trash2Icon } from "lucide-react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";

interface DeleteEventDialogProps {
  title: string;
  onConfirm: () => void;
  /** While the delete request runs, so nobody sends it twice. */
  isDeleting?: boolean;
}

/** The trash can button and the dialog that asks before the event is deleted. */
export function DeleteEventDialog({
  title,
  onConfirm,
  isDeleting,
}: DeleteEventDialogProps) {
  return (
    // The whole row is a link, and React also passes clicks inside the dialog
    // up to it: preventDefault stops the link from opening the editor. It must
    // not sit on the trigger itself, Radix would then not open the dialog.
    <span className="inline-flex" onClick={(e) => e.preventDefault()}>
      <AlertDialog>
        <AlertDialogTrigger asChild>
          <Button
            type="button"
            variant="outline"
            size="icon-sm"
            aria-label={`Event „${title}“ löschen`}
            title="Löschen"
            disabled={isDeleting}
            className="text-destructive hover:cursor-pointer hover:text-destructive"
          >
            <Trash2Icon />
          </Button>
        </AlertDialogTrigger>

        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Event löschen?</AlertDialogTitle>
            <AlertDialogDescription className="wrap-anywhere">
              „{title}“ wird endgültig gelöscht.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="hover:cursor-pointer">
              Abbrechen
            </AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              className="hover:cursor-pointer"
              onClick={onConfirm}
            >
              Löschen
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </span>
  );
}
