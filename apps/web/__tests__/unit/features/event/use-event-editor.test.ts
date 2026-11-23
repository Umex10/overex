import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook } from "@testing-library/react";
import { useEventEditor } from "@/features/event/hooks/use-event-editor";
import type { EventEditorProps } from "@/components/EventEditor";
import type {
  CategoryDto,
  EventDetailsDto,
  EventStatus,
} from "@/features/event/event-types";
import type { EventFormInput } from "@/features/event/event-validation";
import type { ApiErrorBody } from "@/types/api-error";

// --- Mocks -----------------------------------------------------------------

const useGetCategoriesQuery = vi.fn();
const createEvent = vi.fn();
const updateEvent = vi.fn();
const checkImage = vi.fn();
const push = vi.fn();
const toastLoading = vi.fn();
const toastSuccess = vi.fn();
const toastError = vi.fn();

vi.mock("@/features/event/category-api", () => ({
  useGetCategoriesQuery: (...args: unknown[]) => useGetCategoriesQuery(...args),
}));

vi.mock("@/features/event/event-api", () => ({
  useCreateEventMutation: () => [createEvent],
  useUpdateEventMutation: () => [updateEvent],
  useCheckImageUrlMutation: () => [checkImage, { isLoading: false }],
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

vi.mock("sonner", () => ({
  toast: {
    loading: (...args: unknown[]) => toastLoading(...args),
    success: (...args: unknown[]) => toastSuccess(...args),
    error: (...args: unknown[]) => toastError(...args),
  },
}));

// --- Test data -------------------------------------------------------------

/** The frozen "now" of every test: the event below lies in the future. */
const NOW = "2026-06-01T12:00:00Z";

const DEFAULT_IMAGE = "/default.svg";
const IMAGE_URL = "https://example.com/image.png";
const OTHER_IMAGE_URL = "https://example.com/other.png";
/** Same delay as in the hook: the image is checked after this pause. */
const IMAGE_CHECK_DELAY_MS = 600;

const TOAST_ID = "toast-id";

const concert: CategoryDto = { id: "concert", title: "Concert" };
const festival: CategoryDto = { id: "festival", title: "Festival" };

// Built from local parts, so the tests pass in every time zone.
const startsAt = new Date(2026, 11, 31, 20, 30).toISOString();
const saleStartsAt = new Date(2026, 8, 1, 10, 0).toISOString();

/** A completely and correctly filled form. */
const validForm: EventFormInput = {
  title: "Summer Festival",
  categoryId: festival.id,
  description: "Open-air festival.",
  imageUrl: IMAGE_URL,
  startDate: new Date(2026, 11, 31),
  startTime: "20:30",
  venue: "Donauinsel",
  city: "Wien",
  saleDate: new Date(2026, 8, 1),
  saleTime: "10:00",
  status: "DRAFT",
};

/** What the backend gets for `validForm`: one point in time per date + time. */
const validRequestBody = {
  title: "Summer Festival",
  categoryId: festival.id,
  description: "Open-air festival.",
  imageUrl: IMAGE_URL,
  startsAt,
  saleStartsAt,
  venue: "Donauinsel",
  city: "Wien",
  status: "DRAFT",
};

/** The stored event of the edit mode; its form values are `validForm`. */
const storedEvent: EventDetailsDto = {
  event: {
    id: "event-1",
    category: festival,
    title: "Summer Festival",
    description: "Open-air festival.",
    venue: "Donauinsel",
    city: "Wien",
    startsAt,
    saleStartsAt,
    imageUrl: IMAGE_URL,
    status: "DRAFT",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    version: 3,
  },
  statusOptions: {
    DRAFT: { disabled: false, why: null },
    PUBLISHED: { disabled: true, why: "Not allowed." },
    CANCELED: { disabled: true, why: "Not allowed either." },
  },
};

const createMode: EventEditorProps = { mode: "CREATE" };
const editMode: EventEditorProps = { mode: "EDIT", formData: storedEvent };

// --- Arrange helpers -------------------------------------------------------

function categoriesAreLoaded() {
  useGetCategoriesQuery.mockReturnValue({ data: [concert, festival] });
}

/** Both mutations answer with the stored event. */
function backendSavesTheEvent() {
  const response = { unwrap: async () => storedEvent.event };

  createEvent.mockReturnValue(response);
  updateEvent.mockReturnValue(response);
}

/** Both mutations are rejected; without a body the backend is not reachable. */
function backendRejectsTheEvent(data?: ApiErrorBody) {
  const response = { unwrap: () => Promise.reject({ data }) };

  createEvent.mockReturnValue(response);
  updateEvent.mockReturnValue(response);
}

function imageLoads() {
  checkImage.mockImplementation((url: string) => ({
    unwrap: async () => url,
  }));
}

function imageDoesNotLoad() {
  checkImage.mockReturnValue({
    unwrap: () => Promise.reject({ status: "CUSTOM_ERROR" }),
  });
}

/** The first image check answers only when the returned function is called. */
function firstImageLoadsSlowly() {
  const { promise, resolve } = Promise.withResolvers<string>();
  imageLoads();
  checkImage.mockReturnValueOnce({ unwrap: () => promise });

  return () => resolve(IMAGE_URL);
}

// --- Helpers ---------------------------------------------------------------

type Editor = { current: ReturnType<typeof useEventEditor> };

/** `result.current` is always the latest render. */
function renderUseEventEditor(props: EventEditorProps): Editor {
  const { result } = renderHook(() => useEventEditor(props));

  return result;
}

function fillForm(result: Editor, values: Partial<EventFormInput>) {
  act(() => {
    result.current.form.reset({ ...result.current.form.getValues(), ...values });
  });
}

/** Submits with the given button and waits for the answer of the backend. */
async function submit(result: Editor, button: "publish" | "edit") {
  await act(async () => {
    await result.current[button]();
    await vi.advanceTimersByTimeAsync(0);
  });
}

async function stopTypingFor(ms: number) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });
}

function fieldError(result: Editor, field: keyof EventFormInput) {
  return result.current.form.getFieldState(field).error?.message;
}

// --- Setup -----------------------------------------------------------------

beforeEach(() => {
  vi.resetAllMocks();
  // Replaces the real clock and timers: Date is NOW, the debounce of the image
  // check only passes with stopTypingFor().
  vi.useFakeTimers();
  vi.setSystemTime(new Date(NOW));

  toastLoading.mockReturnValue(TOAST_ID);
});

afterEach(() => {
  // Gives the real clock back, so no other test file sees the frozen time.
  vi.useRealTimers();
});

// --- Tests -----------------------------------------------------------------

describe("useEventEditor: initial values", () => {
  it("starts with an empty draft when creating", () => {
    // Arrange
    categoriesAreLoaded();

    // Act
    const result = renderUseEventEditor(createMode);

    // Assert
    expect(result.current.form.getValues()).toEqual({
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
    });
  });

  it("starts with the stored event when editing", () => {
    // Arrange
    categoriesAreLoaded();

    // Act
    const result = renderUseEventEditor(editMode);

    // Assert
    expect(result.current.form.getValues()).toEqual(validForm);
  });

  it("allows every status except canceled when creating", () => {
    // Arrange
    categoriesAreLoaded();

    // Act
    const result = renderUseEventEditor(createMode);

    // Assert
    expect(result.current.statusOptions).toEqual({
      DRAFT: { disabled: false, why: null },
      PUBLISHED: { disabled: false, why: null },
      CANCELED: { disabled: true, why: expect.any(String) },
    });
  });

  it("takes the status options of the backend when editing", () => {
    // Arrange
    categoriesAreLoaded();

    // Act
    const result = renderUseEventEditor(editMode);

    // Assert
    expect(result.current.statusOptions).toEqual(storedEvent.statusOptions);
  });
});

describe("useEventEditor: preview", () => {
  it("shows placeholders while the form is empty", () => {
    // Arrange
    categoriesAreLoaded();

    // Act
    const result = renderUseEventEditor(createMode);

    // Assert
    expect(result.current).toMatchObject({
      title: "Titel des Events",
      venue: "Veranstaltungsort",
      city: "Stadt",
      startsAt: undefined,
      saleStartsAt: undefined,
      categorySelected: undefined,
      previewImageUrl: DEFAULT_IMAGE,
    });
  });

  it("shows the typed values", () => {
    // Arrange
    categoriesAreLoaded();
    const result = renderUseEventEditor(createMode);

    // Act
    fillForm(result, validForm);

    // Assert
    expect(result.current).toMatchObject({
      title: "Summer Festival",
      venue: "Donauinsel",
      city: "Wien",
      status: "DRAFT",
      startsAt,
      saleStartsAt,
      categorySelected: festival,
    });
  });

  it.each([
    {
      missing: "time",
      values: {
        startDate: validForm.startDate,
        saleDate: validForm.saleDate,
      },
    },
    {
      missing: "date",
      values: {
        startTime: validForm.startTime,
        saleTime: validForm.saleTime,
      },
    },
  ])("shows no start and sale start while the $missing is missing", ({ values }) => {
    // Arrange
    categoriesAreLoaded();
    const result = renderUseEventEditor(createMode);

    // Act
    fillForm(result, values);

    // Assert
    expect(result.current.startsAt).toBeUndefined();
    expect(result.current.saleStartsAt).toBeUndefined();
  });

  it("has no categories and no selected one until they are loaded", () => {
    // Arrange
    useGetCategoriesQuery.mockReturnValue({ data: undefined });

    // Act
    const result = renderUseEventEditor(editMode);

    // Assert
    expect(result.current.categories).toEqual([]);
    expect(result.current.categorySelected).toBeUndefined();
  });
});

describe("useEventEditor: image check", () => {
  it("shows the image once it has loaded", async () => {
    // Arrange
    categoriesAreLoaded();
    imageLoads();
    const result = renderUseEventEditor(createMode);

    // Act
    fillForm(result, { imageUrl: IMAGE_URL });
    await stopTypingFor(IMAGE_CHECK_DELAY_MS);

    // Assert
    expect(checkImage).toHaveBeenLastCalledWith(IMAGE_URL);
    expect(result.current.previewImageUrl).toBe(IMAGE_URL);
  });

  it("keeps the default image when the image does not load", async () => {
    // Arrange
    categoriesAreLoaded();
    imageDoesNotLoad();
    const result = renderUseEventEditor(createMode);

    // Act
    fillForm(result, { imageUrl: IMAGE_URL });
    await stopTypingFor(IMAGE_CHECK_DELAY_MS);

    // Assert
    expect(checkImage).toHaveBeenLastCalledWith(IMAGE_URL);
    expect(result.current.previewImageUrl).toBe(DEFAULT_IMAGE);
  });

  it("does not check a half-typed URL", async () => {
    // Arrange
    categoriesAreLoaded();
    const result = renderUseEventEditor(createMode);

    // Act
    fillForm(result, { imageUrl: "htt" });
    await stopTypingFor(IMAGE_CHECK_DELAY_MS);

    // Assert
    expect(checkImage).not.toHaveBeenCalled();
  });

  it("does not check before the user has stopped typing long enough", async () => {
    // Arrange
    categoriesAreLoaded();
    const result = renderUseEventEditor(createMode);

    // Act
    fillForm(result, { imageUrl: IMAGE_URL });
    await stopTypingFor(IMAGE_CHECK_DELAY_MS - 1);

    // Assert
    expect(checkImage).not.toHaveBeenCalled();
  });

  it("only checks the last URL when the user keeps typing", async () => {
    // Arrange
    categoriesAreLoaded();
    imageLoads();
    const result = renderUseEventEditor(createMode);

    // Act
    fillForm(result, { imageUrl: IMAGE_URL });
    await stopTypingFor(IMAGE_CHECK_DELAY_MS - 1);
    fillForm(result, { imageUrl: OTHER_IMAGE_URL });
    await stopTypingFor(IMAGE_CHECK_DELAY_MS);

    // Assert
    expect(checkImage).toHaveBeenCalledTimes(1);
    expect(checkImage).toHaveBeenLastCalledWith(OTHER_IMAGE_URL);
  });

  it("keeps the newer image when an older check answers later", async () => {
    // Arrange: the first URL is still being checked when the second one loads
    categoriesAreLoaded();
    const answerSlowCheck = firstImageLoadsSlowly();
    const result = renderUseEventEditor(createMode);
    fillForm(result, { imageUrl: IMAGE_URL });
    await stopTypingFor(IMAGE_CHECK_DELAY_MS);
    fillForm(result, { imageUrl: OTHER_IMAGE_URL });
    await stopTypingFor(IMAGE_CHECK_DELAY_MS);

    // Act
    await act(async () => answerSlowCheck());

    // Assert
    expect(result.current.previewImageUrl).toBe(OTHER_IMAGE_URL);
  });

  it("goes back to the default image when the URL changes", async () => {
    // Arrange: the first image is shown
    categoriesAreLoaded();
    imageLoads();
    const result = renderUseEventEditor(createMode);
    fillForm(result, { imageUrl: IMAGE_URL });
    await stopTypingFor(IMAGE_CHECK_DELAY_MS);

    // Act
    fillForm(result, { imageUrl: OTHER_IMAGE_URL });

    // Assert
    expect(result.current.previewImageUrl).toBe(DEFAULT_IMAGE);
  });
});

describe("useEventEditor: create", () => {
  it.each<{ status: EventStatus; loading: string; success: string }>([
    {
      status: "DRAFT",
      loading: "Entwurf wird gespeichert …",
      success: "Entwurf gespeichert!",
    },
    {
      status: "PUBLISHED",
      loading: "Event wird veröffentlicht …",
      success: "Event veröffentlicht!",
    },
  ])("creates the event as $status", async ({ status, loading, success }) => {
    // Arrange
    categoriesAreLoaded();
    backendSavesTheEvent();
    const result = renderUseEventEditor(createMode);
    fillForm(result, { ...validForm, status });

    // Act
    await submit(result, "publish");

    // Assert
    expect(createEvent).toHaveBeenLastCalledWith({
      ...validRequestBody,
      status,
    });
    expect(updateEvent).not.toHaveBeenCalled();
    expect(toastLoading).toHaveBeenLastCalledWith(loading, expect.anything());
    expect(toastSuccess).toHaveBeenLastCalledWith(
      success,
      expect.objectContaining({ id: TOAST_ID }),
    );
  });
});

describe("useEventEditor: edit", () => {
  it("sends the changes with the version that was loaded", async () => {
    // Arrange
    categoriesAreLoaded();
    backendSavesTheEvent();
    const result = renderUseEventEditor(editMode);
    fillForm(result, { title: "Winter Festival" });

    // Act
    await submit(result, "edit");

    // Assert
    expect(updateEvent).toHaveBeenLastCalledWith({
      eventId: storedEvent.event.id,
      body: {
        ...validRequestBody,
        title: "Winter Festival",
        version: storedEvent.event.version,
      },
    });
    expect(createEvent).not.toHaveBeenCalled();
    expect(toastSuccess).toHaveBeenLastCalledWith(
      "Änderungen wurden gespeichert",
      expect.objectContaining({ id: TOAST_ID }),
    );
  });

  it("sends nothing when there is no stored event", async () => {
    // Arrange
    categoriesAreLoaded();
    const result = renderUseEventEditor(createMode);
    fillForm(result, validForm);

    // Act
    await submit(result, "edit");

    // Assert
    expect(updateEvent).not.toHaveBeenCalled();
    expect(toastLoading).not.toHaveBeenCalled();
  });
});

// Both submits handle the answer of the backend the same way.
describe.each([
  { button: "publish" as const, props: createMode, mutation: createEvent },
  { button: "edit" as const, props: editMode, mutation: updateEvent },
])("useEventEditor: answer to $button", ({ button, props, mutation }) => {
  it("opens the saved event", async () => {
    // Arrange
    categoriesAreLoaded();
    backendSavesTheEvent();
    const result = renderUseEventEditor(props);
    fillForm(result, validForm);

    // Act
    await submit(result, button);

    // Assert
    expect(push).toHaveBeenLastCalledWith(`/events/${storedEvent.event.id}`);
    expect(toastError).not.toHaveBeenCalled();
  });

  it("shows the field errors and sends nothing when the form is invalid", async () => {
    // Arrange
    categoriesAreLoaded();
    const result = renderUseEventEditor(props);
    fillForm(result, { ...validForm, title: "" });

    // Act
    await submit(result, button);

    // Assert
    expect(fieldError(result, "title")).toBe("Title is required.");
    expect(mutation).not.toHaveBeenCalled();
    expect(toastLoading).not.toHaveBeenCalled();
  });

  it("shows the message of the backend and stays on the page", async () => {
    // Arrange
    categoriesAreLoaded();
    backendRejectsTheEvent({
      status: 409,
      message: "This resource was changed in the meantime.",
      errors: [],
    });
    const result = renderUseEventEditor(props);
    fillForm(result, validForm);

    // Act
    await submit(result, button);

    // Assert
    expect(toastError).toHaveBeenLastCalledWith(
      "This resource was changed in the meantime.",
      expect.objectContaining({ id: TOAST_ID }),
    );
    expect(toastSuccess).not.toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
  });

  it("shows a general message when the backend is not reachable", async () => {
    // Arrange
    categoriesAreLoaded();
    backendRejectsTheEvent();
    const result = renderUseEventEditor(props);
    fillForm(result, validForm);

    // Act
    await submit(result, button);

    // Assert
    expect(toastError).toHaveBeenLastCalledWith(
      "Das Event konnte nicht erstellt werden.",
      expect.objectContaining({ id: TOAST_ID }),
    );
    expect(push).not.toHaveBeenCalled();
  });

  it.each<{ backendField: string; formField: keyof EventFormInput }>([
    { backendField: "startsAt", formField: "startDate" },
    { backendField: "saleStartsAt", formField: "saleDate" },
    { backendField: "title", formField: "title" },
  ])(
    "shows the backend's error on $backendField under $formField",
    async ({ backendField, formField }) => {
      // Arrange
      categoriesAreLoaded();
      backendRejectsTheEvent({
        status: 400,
        message: "Validation failed",
        errors: [{ field: backendField, message: "Rejected by the backend." }],
      });
      const result = renderUseEventEditor(props);
      fillForm(result, validForm);

      // Act
      await submit(result, button);

      // Assert
      expect(fieldError(result, formField)).toBe("Rejected by the backend.");
    },
  );
});
