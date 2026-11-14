"use client";

import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  useCheckImageUrlMutation,
  useCreateEventMutation,
  useUpdateEventMutation,
} from "@/features/event/event-api";

import type { ApiErrorBody } from "@/types/api-error";
import type {
  EventDto,
  EventStatus,
  StatusOptionMap,
} from "@/features/event/event-types";
import {
  combineDateAndTime,
  eventSchema,
  uncombineDateAndTime,
  type EventFormInput,
  type EventFormValues,
} from "@/features/event/event-validation";
import { useEffect, useState } from "react";
import { useGetCategoriesQuery } from "../category-api";
import type { EventEditorProps } from "@/components/EventEditor";

type DefaultFormValues = {
  title: string;
  categoryId: string;
  description: string;
  imageUrl: string;
  startDate: Date | undefined;
  startTime: string;
  venue: string;
  city: string;
  saleDate: Date | undefined;
  saleTime: string;
  status: EventStatus;
};

type Status = "DRAFT" | "PUBLISHED" | "CANCELED";

/** Backend field names that differ from the form's field names. */
const BACKEND_TO_FORM_FIELD: Record<string, keyof EventFormInput> = {
  startsAt: "startDate",
  saleStartsAt: "saleDate",
};

/** Shown in the preview until a typed image URL has loaded successfully. */
const DEFAULT_IMAGE = "/default.svg";
/** Debounce: the image is only checked after the user stopped typing this long. */
const IMAGE_CHECK_DELAY_MS = 600;

/**
 * Status options of a new event. There is no stored event yet, so the backend
 * can't send any: a new event starts as draft or published, never canceled.
 */
const CREATE_STATUS_OPTIONS: StatusOptionMap = {
  DRAFT: { disabled: false, why: null },
  PUBLISHED: { disabled: false, why: null },
  CANCELED: { disabled: true, why: "A new event can't be canceled." },
};

export function useEventEditor(props: EventEditorProps) {
  const { data: categories = [] } = useGetCategoriesQuery();
  // Only EDIT has a stored event; checking `mode` narrows `props`, so
  // `props.formData` is known to exist in the EDIT branch.
  const event = props.mode === "EDIT" ? props.formData.event : undefined;
  const statusOptions: StatusOptionMap =
    props.mode === "EDIT" ? props.formData.statusOptions : CREATE_STATUS_OPTIONS;

  let defaultValues: DefaultFormValues = {
    title: "",
    categoryId: "",
    description: "",
    imageUrl: "",
    startDate: undefined,
    startTime: "",
    venue: "",
    city: "",
    saleDate: undefined,
    saleTime: "",
    status: "DRAFT",
  };

  if (
    event &&
    !Object.values(event).every(
      (v) => v === undefined || v === null || v === "",
    )
  ) {
    const { date: startDate, time: startTime } = uncombineDateAndTime(
      event.startsAt,
    );
    const { date: saleDate, time: saleTime } = uncombineDateAndTime(
      event.saleStartsAt,
    );
    // `version` isn't a form field: the submit takes it from props.formData directly.
    const { id, createdAt, updatedAt, version, ...rest } = event;

    defaultValues = {
      startDate,
      startTime,
      saleDate,
      saleTime,
      categoryId: rest.category.id,
      ...rest,
    };
  }

  // Holds all form values and errors; the <Controller>s below read and write it.
  const form = useForm<EventFormInput, unknown, EventFormValues>({
    resolver: zodResolver(eventSchema),
    mode: "onTouched",
    defaultValues,
  });

  // Live values for the preview card. `||` (not `??`) so an empty string also
  // falls back to the placeholder, `??` would only catch undefined/null.
  const title =
    useWatch({ control: form.control, name: "title" }) || "Titel des Events";
  const categoryId = useWatch({ control: form.control, name: "categoryId" });
  const rawImageUrl = useWatch({ control: form.control, name: "imageUrl" });
  // Only a complete URL is worth checking, half-typed input ("htt…") isn't.
  const candidateImageUrl =
    rawImageUrl && URL.canParse(rawImageUrl) ? rawImageUrl : undefined;
  // The last URL that really loaded an image.
  const [verifiedImageUrl, setVerifiedImageUrl] = useState<string>();
  const [checkImage, { isLoading: isCheckingImage }] =
    useCheckImageUrlMutation();

  const startDate = useWatch({ control: form.control, name: "startDate" });
  const startTime = useWatch({ control: form.control, name: "startTime" });
  // One point in time for DateFormat / TimeFormat, only once both are picked.
  const startsAt =
    startDate && startTime
      ? combineDateAndTime(startDate, startTime).toISOString()
      : undefined;
  const venue =
    useWatch({ control: form.control, name: "venue" }) || "Veranstaltungsort";
  const city = useWatch({ control: form.control, name: "city" }) || "Stadt";
  const saleDate = useWatch({ control: form.control, name: "saleDate" });
  const saleTime = useWatch({ control: form.control, name: "saleTime" });
  // Same as startsAt, for the ticket sale start in the preview.
  const saleStartsAt =
    saleDate && saleTime
      ? combineDateAndTime(saleDate, saleTime).toISOString()
      : undefined;
  const status = useWatch({ control: form.control, name: "status" });

  const categorySelected = categories.find((c) => c.id === categoryId);

  const router = useRouter();
  const [createEvent] = useCreateEventMutation();
  const [updateEvent] = useUpdateEventMutation();

  // Debounced image check: every keystroke restarts the timer (cleanup
  // below), so the check only runs once the user paused typing.
  useEffect(() => {
    if (!candidateImageUrl) return;

    // Set by the cleanup when the URL changed in the meantime, so an older,
    // slower check can't overwrite the result of a newer one.
    let cancelled = false;

    const timer = setTimeout(async () => {
      try {
        const url = await checkImage(candidateImageUrl).unwrap();
        if (!cancelled) setVerifiedImageUrl(url);
      } catch {
        // Not an image or not reachable: the preview keeps the default.
      }
    }, IMAGE_CHECK_DELAY_MS);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [candidateImageUrl, checkImage]);

  // Only the URL in the field right now counts, an older verified one doesn't.
  const previewImageUrl =
    candidateImageUrl && verifiedImageUrl === candidateImageUrl
      ? candidateImageUrl
      : DEFAULT_IMAGE;

  // Sends the validated values with the status of the clicked button.
  // `values` already has the shape of CreateEventDto minus the status
  // (startsAt / saleStartsAt as ISO strings).
  const submitCreateEvent = async (values: EventFormValues) => {
    const isDraft = values.status === "DRAFT";
    const toastId = toast.loading(
      isDraft ? "Entwurf wird gespeichert …" : "Event wird veröffentlicht …",
      { className: "toast-loading" },
    );
    try {
      // unwrap(): returns the created event, or throws the error response.
      // const created = mode === "CREATE" ? await createEvent({ ...values, status }).unwrap()
      // : await updateEvent({...values, _status}).unwrap();
      const created = await createEvent(values).unwrap();

      toast.success(
        isDraft ? "Entwurf gespeichert!" : "Event veröffentlicht!",
        {
          id: toastId,
          className: "toast-success",
        },
      );
      router.push(`/events/${created.id}`);
    } catch (error) {
      // RTK Query puts the backend's error body into `data`.
      const apiError = (error as { data?: ApiErrorBody }).data;
      toast.error(
        apiError?.message || "Das Event konnte nicht erstellt werden.",
        {
          id: toastId,
          className: "toast-error",
        },
      );

      // Show backend field errors under the matching input. The backend knows
      // startsAt / saleStartsAt, the form has date and time separately.
      apiError?.errors?.forEach(({ field, message }) => {
        const formField = BACKEND_TO_FORM_FIELD[field] ?? field;
        form.setError(formField as keyof EventFormInput, { message });
      });
    }
  };

  const submitEditEvent = async (
    values: EventFormValues,
  ) => {
    // Only reachable through the Save button, which EDIT mode shows. The check
    // narrows `props`, so the stored event's id and version need no `!`.
    if (props.mode !== "EDIT") return;
    const { id, version } = props.formData.event;

    console.log(values.status);

    const toastId = toast.loading("Die Änderungen werden gespeichert", {
      className: "toast-loading",
    });
    try {
      // unwrap(): returns the created event, or throws the error response.
      // const created = mode === "CREATE" ? await createEvent({ ...values, status }).unwrap()
      // : await updateEvent({...values, _status}).unwrap();
      const created = await updateEvent({
        eventId: id,
        body: { ...values, version },
      }).unwrap();

      toast.success("Änderungen wurden gespeichert", {
        id: toastId,
        className: "toast-success",
      });
      router.push(`/events/${created.id}`);
    } catch (error) {
      // RTK Query puts the backend's error body into `data`.
      const apiError = (error as { data?: ApiErrorBody }).data;
      toast.error(
        apiError?.message || "Das Event konnte nicht erstellt werden.",
        {
          id: toastId,
          className: "toast-error",
        },
      );

      // Show backend field errors under the matching input. The backend knows
      // startsAt / saleStartsAt, the form has date and time separately.
      apiError?.errors?.forEach(({ field, message }) => {
        const formField = BACKEND_TO_FORM_FIELD[field] ?? field;
        form.setError(formField as keyof EventFormInput, { message });
      });
    }
  };

  const publish = form.handleSubmit((values) => {
    submitCreateEvent(values);
  });
  const edit = form.handleSubmit((values) =>
    submitEditEvent(values),
  );

  return {
    publish,
    edit,
    form,
    categories,
    isCheckingImage,
    previewImageUrl,
    categorySelected,
    title,
    startsAt,
    venue,
    city,
    saleStartsAt,
    status,
    statusOptions
  };
}

/**
 * The `form` that {@link useEventEditor} returns, for components that get it
 * as a prop. Derived from the hook, so it never has to be written out by hand.
 */
export type EventEditorForm = ReturnType<typeof useEventEditor>["form"];
