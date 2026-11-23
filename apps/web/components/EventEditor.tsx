"use client";

import { Controller } from "react-hook-form";
import { Separator } from "@/components/ui/separator";

import { RequiredMark } from "@/components/required-mark";
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

import { Skeleton } from "@/components/ui/skeleton";
import {
  SECTION_CLASS,
  SIDE_COLUMN_CLASS,
} from "@/components/event-editor-layout";
import type { EventDetailsDto } from "@/features/event/event-types";
import { EventEditorButtons } from "@/features/event/components/event-editor-buttons";
import { DateTimeFields } from "@/features/event/components/date-time-fields";

import BreadcrumbsContainer from "@/features/shell/components/breadcrumbs-container";
import { EventStatusBadge } from "@/features/event/components/event-status-badge";
import { CalendarIcon, MapPinIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import Image from "next/image";
import EventMetaItem from "@/features/event/components/event-meta-item";
import DateFormat from "@/features/event/components/date-format";
import TimeFormat from "@/features/event/components/time-format";
import { useIsMobile } from "@/hooks/use-mobile";
import { useEventEditor } from "@/features/event/hooks/use-event-editor";

/**
 * CREATE starts empty; EDIT always comes with the loaded event and its status
 * options. A union instead of an optional `formData`: `mode="EDIT"` without
 * `formData` doesn't compile, and after checking `mode === "EDIT"` TypeScript
 * knows `formData` is there, no `!` or `?.` needed.
 */
export type EventEditorProps =
  | { mode: "CREATE" }
  | { mode: "EDIT"; formData: EventDetailsDto };

export type EventEditorMode = EventEditorProps["mode"];

export default function EventEditor(props: EventEditorProps) {
  const { mode } = props;
  const isMobile = useIsMobile();

  const {
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
  } = useEventEditor(props);

  return (
    <div className="w-full flex w-full flex-col gap-3">
      {isMobile ? (
        <div className="flex flex-col gap-3">
          {isMobile && <BreadcrumbsContainer></BreadcrumbsContainer>}
          <h3 className="text-lg font-extrabold">All Events</h3>
        </div>
      ) : (
        <div className="w-full flex flex-row justify-between items-center">
          <h3 className="text-lg font-extrabold">All Events</h3>

          <div
            className={cn("flex flex-row items-end gap-2", SIDE_COLUMN_CLASS)}
          >
            {/* <SaveDraftButton
              onSaveDraft={saveDraft}
              disabled={form.formState.isSubmitting}
            ></SaveDraftButton> */}

            <EventEditorButtons
              form={form}
              mode={mode}
              statusOptionsMap={statusOptions}
              onPublish={publish}
              onEdit={edit}
            ></EventEditorButtons>
          </div>
        </div>
      )}

      <div className="flex flex-col gap-3 lg:flex-row lg:items-start">
        {/* noValidate: the zod messages replace the browser's own bubbles. */}
        <form
          id="create-event-form"
          noValidate
          className="flex w-full min-w-0 flex-col gap-4"
        >
          {/* ---------- Grunddaten ---------- */}
          <FieldSet className={SECTION_CLASS}>
            <FieldLegend className="flex w-full items-center justify-between text-base font-bold">
              <span>Grunddaten</span>
              <span className="flex items-center gap-0.5 text-sm font-extralight">
                <RequiredMark />
                Pflichtfeld
              </span>
            </FieldLegend>

            <FieldGroup className="gap-3">
              {/* Title */}
              <Controller
                name="title"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid} className="gap-1">
                    <FieldLabel htmlFor="title" className="text-base">
                      Title <RequiredMark />
                    </FieldLabel>
                    <Input
                      {...field}
                      id="title"
                      required
                      placeholder="f. e. Nordhafen FC – SV Eichenau"
                      aria-invalid={fieldState.invalid}
                    />
                    <FieldError errors={[fieldState.error]} />
                  </Field>
                )}
              />

              {/* Category: Radix Select has no onChange, so no {...field} here. */}
              <Controller
                name="categoryId"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid} className="gap-1">
                    <FieldLabel htmlFor="category" className="text-base">
                      Category <RequiredMark />
                    </FieldLabel>
                    <Select
                      name={field.name}
                      value={field.value}
                      onValueChange={field.onChange}
                    >
                      <SelectTrigger
                        id="category"
                        onBlur={field.onBlur}
                        aria-invalid={fieldState.invalid}
                        className="w-full"
                      >
                        <SelectValue placeholder="Choose a category" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectGroup>
                          {categories.map((category) => (
                            <SelectItem key={category.id} value={category.id}>
                              {category.title}
                            </SelectItem>
                          ))}
                        </SelectGroup>
                      </SelectContent>
                    </Select>
                    <FieldError errors={[fieldState.error]} />
                  </Field>
                )}
              />

              {/* Description */}
              <Controller
                name="description"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid} className="gap-1">
                    <FieldLabel htmlFor="description" className="text-base">
                      Beschreibung <RequiredMark />
                    </FieldLabel>
                    <Textarea
                      {...field}
                      id="description"
                      required
                      placeholder="What may a visitor expect?"
                      aria-invalid={fieldState.invalid}
                    />
                    <FieldError errors={[fieldState.error]} />
                  </Field>
                )}
              />

              {/* Image URL */}
              <Controller
                name="imageUrl"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid} className="gap-1">
                    <FieldLabel htmlFor="image-url" className="text-base">
                      Image-URL <RequiredMark />
                    </FieldLabel>
                    <Input
                      {...field}
                      id="image-url"
                      type="url"
                      required
                      placeholder="https://"
                      aria-invalid={fieldState.invalid}
                    />
                    <FieldDescription className="text-sm">
                      Direct link to an image in 16:9 format (JPG, PNG or WebP)
                    </FieldDescription>
                    <FieldError errors={[fieldState.error]} />
                  </Field>
                )}
              />

              {/* Event start: date + time */}
              <DateTimeFields
                control={form.control}
                dateName="startDate"
                timeName="startTime"
                idPrefix="start"
                dateLabel="Date"
                timeLabel="Time"
              />

              {/* Venue */}
              <Controller
                name="venue"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid} className="gap-1">
                    <FieldLabel htmlFor="venue" className="text-base">
                      Venue <RequiredMark />
                    </FieldLabel>
                    <Input
                      {...field}
                      id="venue"
                      required
                      placeholder="f. e. Santiago Bernabéu"
                      aria-invalid={fieldState.invalid}
                    />
                    <FieldError errors={[fieldState.error]} />
                  </Field>
                )}
              />

              {/* City */}
              <Controller
                name="city"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid} className="gap-1">
                    <FieldLabel htmlFor="city" className="text-base">
                      City <RequiredMark />
                    </FieldLabel>
                    <Input
                      {...field}
                      id="city"
                      required
                      placeholder="f. e. Madrid"
                      aria-invalid={fieldState.invalid}
                    />
                    <FieldError errors={[fieldState.error]} />
                  </Field>
                )}
              />
            </FieldGroup>
          </FieldSet>

          {/* ---------- Verkaufsstart ---------- */}
          <FieldSet className={SECTION_CLASS}>
            <FieldLegend className="mb-0 text-base font-bold">
              Verkaufsstart
            </FieldLegend>
            <FieldDescription className="text-sm">
              From this moment on the buy button is active. Before that, the
              countdown runs.
            </FieldDescription>

            <DateTimeFields
              control={form.control}
              dateName="saleDate"
              timeName="saleTime"
              idPrefix="sale"
              dateLabel="Date"
              timeLabel="Time"
            />
          </FieldSet>
        </form>

        <div
          className={cn(
            SECTION_CLASS,
            "flex w-full flex-col gap-2 lg:sticky lg:top-0",
            SIDE_COLUMN_CLASS,
          )}
        >
          <div className="flex flex-row justify-between items-center opacity-70">
            <h3 className="uppercase text-sm font-bold tracking-wide">
              Live-Vorschau
            </h3>
            <EventStatusBadge status={status} className="p-3 text-sm" />
          </div>

          {/* Same structure and sizes as the card view of EventCard. */}
          <div className="flex flex-col overflow-hidden rounded-xl border bg-card text-card-foreground">
            <div className="relative aspect-video w-full shrink-0 overflow-hidden">
              {isCheckingImage ? (
                <Skeleton className="size-full" />
              ) : (
                <Image
                  src={previewImageUrl}
                  alt="Image of the Event"
                  fill
                  // unoptimized: the browser loads the picture directly, so
                  // any host works here, not only the ones in next.config.
                  unoptimized
                  className="object-cover"
                />
              )}
            </div>

            <div className="flex min-w-0 flex-col gap-1.5 p-4">
              <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
                {categorySelected?.title ?? "Kategorie"}
              </p>
              <h3 className="line-clamp-2 text-lg font-bold">{title}</h3>

              <div className="flex flex-col gap-1 text-sm text-muted-foreground">
                <EventMetaItem icon={CalendarIcon} className="gap-1.5">
                  {startsAt ? (
                    <>
                      <DateFormat dateToFormat={startsAt} weekdayType="short" />{" "}
                      · <TimeFormat timeToFormat={startsAt} />
                    </>
                  ) : (
                    "Datum · Uhrzeit"
                  )}
                </EventMetaItem>
                <EventMetaItem icon={MapPinIcon} className="gap-1.5">
                  {venue}, {city}
                </EventMetaItem>
              </div>
            </div>

            <Separator className="mt-auto" />

            {/* Where the card shows the price, the preview shows the sale
                start: prices don't exist yet when the event is created. */}
            <p className="px-4 py-3 text-sm text-muted-foreground">
              Verkaufsstart:{" "}
              {saleStartsAt ? (
                <>
                  <DateFormat dateToFormat={saleStartsAt} weekdayType="short" />{" "}
                  · <TimeFormat timeToFormat={saleStartsAt} />
                </>
              ) : (
                "Noch nicht festgelegt"
              )}
            </p>
          </div>
          <p className="text-muted-foreground text-sm">
            So erscheint die Karte in der öffentlichen Eventliste.
          </p>
        </div>

        {isMobile && (
          <div className="sticky bottom-0 -mx-4 -mb-4 border-t bg-background p-4">
            <div className="grid grid-cols-2 gap-2 items-end">
              <EventEditorButtons
                form={form}
                mode={mode}
                statusOptionsMap={statusOptions}
                onPublish={publish}
                onEdit={edit}
              ></EventEditorButtons>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
