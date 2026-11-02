import { test, expect } from "@playwright/test";
import { bootstrapAccount, newAccount } from "./utils/accounts";
import {
  expectSignedIn,
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
  test("lets a guest browse the events without signing in", async ({ page }) => {
    // Act
    await page.goto("/events");

    // Assert
    await expect(page).toHaveURL("/events");
  });

  test("signs a new user up and in, without a field to choose a role", async ({ page }) => {
    // Arrange
    const account = newAccount();
    await openSignUp(page);

    // Assert: nobody picks their own role, there is no field to tamper with
    await expect(page.getByTestId("sign-up-role-trigger")).toHaveCount(0);

    // Act
    await submitSignUp(page, account);

    // Assert
    await expect(page.locator(".toast-success")).toBeVisible();
    await expectSignedIn(page);
  });

  test("shows the field errors and sends nothing when the sign-up form is invalid", async ({
    page,
  }) => {
    // Arrange
    await openSignUp(page);
    await page.getByTestId("sign-up-email-input").fill("not-an-email");
    await page.getByTestId("sign-up-password-input").fill("short");

    // Act
    await page.getByTestId("sign-up-submit-button").click();

    // Assert
    await expect(page.getByTestId("sign-up-email-error")).toBeVisible();
    await expect(page.getByTestId("sign-up-password-error")).toContainText("8 characters");
    await expect(page.getByTestId("sign-up-terms-error")).toBeVisible();
    await expect(page).toHaveURL("/sign-up");
  });

  test("marks a taken email and phone, clears the errors on edit and signs up on retry", async ({
    page,
  }) => {
    // Arrange
    const fresh = newAccount();
    await openSignUp(page);

    // Act
    await submitSignUp(page, {
      ...fresh,
      email: bootstrapAccount.email,
      phone: bootstrapAccount.phone,
    });

    // Assert
    await expect(page.locator(".toast-error")).toBeVisible();
    await expect(page.getByTestId("sign-up-email-error")).toBeVisible();
    await expect(page.getByTestId("sign-up-phone-error")).toBeVisible();

    // Act: editing the fields removes the backend's errors
    await page.getByTestId("sign-up-email-input").fill(fresh.email);
    await page.getByTestId("sign-up-phone-input").fill(fresh.phone);

    // Assert
    await expect(page.getByTestId("sign-up-email-error")).toBeHidden();
    await expect(page.getByTestId("sign-up-phone-error")).toBeHidden();

    // Act: the retry with free values succeeds
    await page.getByTestId("sign-up-submit-button").click();

    // Assert
    await expectSignedIn(page);
  });

  test("shows only a general error, not which field was wrong, when the password is wrong", async ({
    page,
  }) => {
    // Arrange
    await openSignIn(page);

    // Act
    await submitSignIn(page, bootstrapAccount.email, "wrong-password");

    // Assert
    await expect(page.locator(".toast-error")).toBeVisible();
    await expect(page.getByTestId("sign-in-email-error")).toBeHidden();
    await expect(page.getByTestId("sign-in-password-error")).toBeHidden();
    await expect(page).toHaveURL("/sign-in");
  });

  test("signs in when the email is typed in upper case", async ({ page }) => {
    // Arrange
    await openSignIn(page);

    // Act
    await submitSignIn(page, bootstrapAccount.email.toUpperCase(), bootstrapAccount.password);

    // Assert
    await expect(page.locator(".toast-success")).toBeVisible();
    await expectSignedIn(page);
  });

  test("deletes the cookie and redirects to sign-in when the backend rejects the refresh token", async ({
    page,
    context,
  }) => {
    // Arrange: e.g. expired, or signed with a key the backend no longer has.
    // /events is a guest page, so the proxy lets it through; the AuthProvider
    // still sends the cookie to the backend, which rejects it.
    await context.addCookies([
      { name: "refresh_tk", value: "not-a-valid-token", url: "http://localhost:3001" },
    ]);

    // Act
    await page.goto("/events");

    // Assert
    await expect(page).toHaveURL("/sign-in?session=expired");
    await expect(page.getByText("Your session has expired.", { exact: false })).toBeVisible();
    // The stale cookie is gone, so the proxy does not bounce back to /events.
    expect((await context.cookies()).some((c) => c.name === "refresh_tk")).toBe(false);
  });
});
