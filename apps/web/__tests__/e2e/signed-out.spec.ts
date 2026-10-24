import { test, expect } from "@playwright/test";
import { bootstrapAccount, newAccount } from "./utils/accounts";
import {
  expectEventFor,
  openSignIn,
  openSignUp,
  submitSignIn,
  submitSignUp,
} from "./utils/flows";

/**
 * Specs that start as a visitor without a session: the storage state from
 * auth.setup.ts is replaced by an empty one.
 */
test.use({ storageState: { cookies: [], origins: [] } });

test.describe("Signed out", () => {
  test("a protected page sends visitors to sign-in", async ({ page }) => {
    await page.goto("/events");

    await expect(page).toHaveURL("/sign-in");
  });

  test("sign-up creates a USER account and shows it on the dashboard", async ({ page }) => {
    const account = newAccount();
    await openSignUp(page);

    // Nobody picks their own role: there is no role field to tamper with.
    await expect(page.getByTestId("sign-up-role-trigger")).toHaveCount(0);

    await submitSignUp(page, account);

    await expect(page.locator(".toast-success")).toBeVisible();
    await expectEventFor(page, account);
  });

  test("the form explains invalid input before anything is sent", async ({ page }) => {
    await openSignUp(page);

    await page.getByTestId("sign-up-email-input").fill("not-an-email");
    await page.getByTestId("sign-up-password-input").fill("short");
    await page.getByTestId("sign-up-submit-button").click();

    await expect(page.getByTestId("sign-up-email-error")).toBeVisible();
    await expect(page.getByTestId("sign-up-password-error")).toContainText("8 characters");
    await expect(page.getByTestId("sign-up-terms-error")).toBeVisible();
    await expect(page).toHaveURL("/sign-up");
  });

  test("taken email and phone are marked on the fields and clear on edit", async ({ page }) => {
    const fresh = newAccount();
    await openSignUp(page);

    await submitSignUp(page, { ...fresh, email: bootstrapAccount.email, phone: bootstrapAccount.phone });

    await expect(page.locator(".toast-error")).toBeVisible();
    await expect(page.getByTestId("sign-up-email-error")).toBeVisible();
    await expect(page.getByTestId("sign-up-phone-error")).toBeVisible();

    // Editing the fields removes the backend's errors, and the retry succeeds.
    await page.getByTestId("sign-up-email-input").fill(fresh.email);
    await page.getByTestId("sign-up-phone-input").fill(fresh.phone);
    await expect(page.getByTestId("sign-up-email-error")).toBeHidden();
    await expect(page.getByTestId("sign-up-phone-error")).toBeHidden();

    await page.getByTestId("sign-up-submit-button").click();
    await expectEventFor(page, fresh);
  });

  test("wrong credentials fail without saying which field was wrong", async ({ page }) => {
    await openSignIn(page);

    await submitSignIn(page, bootstrapAccount.email, "wrong-password");

    await expect(page.locator(".toast-error")).toBeVisible();
    await expect(page.getByTestId("sign-in-email-error")).toBeHidden();
    await expect(page.getByTestId("sign-in-password-error")).toBeHidden();
    await expect(page).toHaveURL("/sign-in");
  });

  test("sign-in works regardless of the email's capitalization", async ({ page }) => {
    await openSignIn(page);

    await submitSignIn(page, bootstrapAccount.email.toUpperCase(), bootstrapAccount.password);

    await expect(page.locator(".toast-success")).toBeVisible();
    await expectEventFor(page, bootstrapAccount);
  });

  test("a rejected refresh cookie is removed and leads back to sign-in", async ({
    page,
    context,
  }) => {
    // E.g. expired, or signed with a key the backend no longer has.
    await context.addCookies([
      { name: "refresh_tk", value: "not-a-valid-token", url: "http://localhost:3001" },
    ]);

    await page.goto("/events");

    await expect(page).toHaveURL("/sign-in?session=expired");
    await expect(page.getByTestId("sign-in-session-expired-note")).toBeVisible();
    // The stale cookie is gone, so the page stays here instead of bouncing back.
    expect((await context.cookies()).some((c) => c.name === "refresh_tk")).toBe(false);
  });
});
