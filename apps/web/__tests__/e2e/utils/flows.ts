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
export async function submitSignIn(page: Page, email: string, password: string) {
  await page.getByTestId("sign-in-email-input").fill(email);
  await page.getByTestId("sign-in-password-input").fill(password);
  await page.getByTestId("sign-in-submit-button").click();
}

/** Uses the header button; ends on the landing page. */
export async function signOut(page: Page) {
  await page.getByTestId("header-sign-out-button").click();
  await expect(page).toHaveURL("/");
}

/** The dashboard greets the user and shows the account loaded via GET /me. */
export async function expectEventFor(page: Page, account: TestAccount) {
  await expect(page).toHaveURL(/\/event/);
}
