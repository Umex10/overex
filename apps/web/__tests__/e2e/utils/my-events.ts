import type { APIResponse, Page } from "@playwright/test";
import { newAccount } from "./accounts";
import { seedCategories } from "./seed";

/*
 * Arrange helpers for "Meine Events": they talk to the backend directly, so a
 * test doesn't have to click its data together first.
 */

const API_URL = "http://localhost:8081/api/v1";
const DAY_MS = 24 * 60 * 60 * 1000;

type Status = "DRAFT" | "PUBLISHED" | "CANCELED";

export interface Organizer {
  page: Page;
  headers: { Authorization: string };
}

export interface CreatedEvent {
  id: string;
  title: string;
  version: number;
}

/** Stops the test right here when the backend rejects an Arrange request. */
async function bodyOf(response: APIResponse) {
  if (!response.ok()) {
    throw new Error(`${response.url()} answered ${response.status()}`);
  }
  return response.json();
}

/**
 * Signs the browser in as a brand-new user without any events. `page.request`
 * shares its cookies with the page, and cookies ignore the port: the refresh
 * cookie of the backend (8081) also counts for the frontend (3001).
 */
export async function signInAsNewOrganizer(page: Page): Promise<Organizer> {
  const response = await page.request.post(`${API_URL}/auth/sign-up`, {
    data: newAccount(),
  });
  const { accessTk } = await bodyOf(response);

  return { page, headers: { Authorization: `Bearer ${accessTk}` } };
}

/** What the backend needs to create an event that starts in `startsInDays`. */
function eventBody(title: string, startsInDays: number, categoryId: string) {
  return {
    title,
    categoryId,
    description: "Created by the e2e tests.",
    venue: "Donauinsel",
    city: "Wien",
    startsAt: new Date(Date.now() + startsInDays * DAY_MS).toISOString(),
    saleStartsAt: new Date(Date.now() + DAY_MS).toISOString(),
    imageUrl: "https://images.unsplash.com/photo-1501386761578-eac5c94b800a",
  };
}

/** Creates a draft of the organizer; the sale opens tomorrow. */
export async function createDraft(
  { page, headers }: Organizer,
  title: string,
  startsInDays = 100,
  categoryId = seedCategories.concert.id,
): Promise<CreatedEvent> {
  const response = await page.request.post(`${API_URL}/events`, {
    headers,
    data: { ...eventBody(title, startsInDays, categoryId), status: "DRAFT" },
  });

  return bodyOf(response);
}

/** Changes the status like the editor would; raises the event's version. */
export async function changeStatus(
  { page, headers }: Organizer,
  event: CreatedEvent,
  status: Status,
): Promise<CreatedEvent> {
  const response = await page.request.patch(`${API_URL}/events/${event.id}`, {
    headers,
    data: {
      ...eventBody(event.title, 100, seedCategories.concert.id),
      status,
      version: event.version,
    },
  });

  return bodyOf(response);
}
