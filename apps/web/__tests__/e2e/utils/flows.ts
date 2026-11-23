import { expect, type Page } from "@playwright/test";
import type { TestAccount } from "./accounts";

/*
 * Reusable steps. Each "open" helper waits until the submit button is enabled:
 * the forms keep it disabled until React has taken over the page, so filling
 * in fields before that point would be lost.
 */

export async function openSignUp(page: Page) {
  await page.goto("/sign-up");
  await expect(page.getByTestId("sign-up-submit-button")).toBeEnabled();
}

export async function openSignIn(page: Page) {
  await page.goto("/sign-in");
  await expect(page.getByTestId("sign-in-submit-button")).toBeEnabled();
}

/** Fills the sign-up form (on the current page) and submits it. */
export async function submitSignUp(page: Page, account: TestAccount) {
  await page.getByTestId("sign-up-name-input").fill(account.name);
  await page.getByTestId("sign-up-email-input").fill(account.email);
  await page.getByTestId("sign-up-phone-input").fill(account.phone);
  await page.getByTestId("sign-up-password-input").fill(account.password);
  await page.getByTestId("sign-up-terms-checkbox").check();
  await page.getByTestId("sign-up-submit-button").click();
}

/** Fills the sign-in form (on the current page) and submits it. */
export async function submitSignIn(
  page: Page,
  email: string,
  password: string,
) {
  await page.getByTestId("sign-in-email-input").fill(email);
  await page.getByTestId("sign-in-password-input").fill(password);
  await page.getByTestId("sign-in-submit-button").click();
}

/** Uses the sidebar button; ends on the sign-in page. */
export async function signOut(page: Page) {
  await page.getByTestId("header-sign-out-button").click();
  await expect(page).toHaveURL("/sign-in");
}

/**
 * Signed in and on the events page. The sidebar only offers "sign out" once
 * GET /me has returned the user, so this also proves the session works.
 */
export async function expectSignedIn(page: Page) {
  await expect(page).toHaveURL(/\/events/);
  await expect(page.getByTestId("header-sign-out-button")).toBeVisible();
}

/** Opens the event list, clicks the card of `event` and waits for its details. */
export async function openEventDetails(
  page: Page,
  event: { id: string; title: string },
) {
  await page.goto("/events");
  await page.getByTestId("event-card").filter({ hasText: event.title }).click();
  await expect(page).toHaveURL(`/events/${event.id}`);
}

/** Opens "Meine Events", clicks the row of `event` and waits for the editor. */
export async function openMyEvent(
  page: Page,
  event: { id: string; title: string },
) {
  await page.goto("/my-events");
  await page
    .getByTestId("my-event-item")
    .filter({ hasText: event.title })
    .getByRole("heading")
    .click();
  await expect(page).toHaveURL(`/my-events/${event.id}`);
}

/**
 * Picks a day of the next month in the date picker, so it is always in the
 * future. Only for days from 7 to 22: others can appear twice, because the
 * calendar also shows the edges of the neighbouring months.
 */
export async function pickDay(page: Page, trigger: string, day: number) {
  await page.locator(trigger).click();
  await page.locator(".rdp-button_next").click();
  await page.locator(`button[data-day^="${day}."]`).click();
  // The calendar closes with an animation: wait, so the next pickDay on the
  // page doesn't find two of them.
  await expect(page.locator(".rdp-button_next")).toHaveCount(0);
}
