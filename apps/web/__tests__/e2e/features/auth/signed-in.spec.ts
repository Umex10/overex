import { test, expect } from "@playwright/test";
import { expectSignedIn, signOut } from "../../utils/flows";

/**
 * Specs that start signed in as the bootstrap account (storage state from
 * auth.setup.ts). Every test gets its own browser context, so signing out in
 * one test does not affect the others.
 */
test.describe("Signed in", () => {
  test("stays signed in after a reload (silent refresh)", async ({ page }) => {
    // Arrange
    await page.goto("/events");
    await expectSignedIn(page);

    // Act: a full page load starts with an empty Redux cache, so the access
    // token has to be fetched again with the refresh cookie before GET /me.
    await page.reload();

    // Assert
    await expectSignedIn(page);
  });

  for (const path of ["/sign-in", "/sign-up"]) {
    test(`forwards from ${path} to the events`, async ({ page }) => {
      // Act
      await page.goto(path);

      // Assert
      await expect(page).toHaveURL("/events");
    });
  }

  test("forwards from the landing page to the events with the 'redirected' flag", async ({
    page,
  }) => {
    // Act
    await page.goto("/");

    // Assert
    await expect(page).toHaveURL("/events?redirected=1");
  });

  test("deletes the refresh cookie and falls back to guest mode when signing out", async ({
    page,
    context,
  }) => {
    // Arrange
    await page.goto("/events");
    await expectSignedIn(page);

    // Act
    await signOut(page);

    // Assert
    expect((await context.cookies()).some((c) => c.name === "refresh_tk")).toBe(false);
    await page.goto("/events");
    await expect(page).toHaveURL("/events"); // guest mode: no redirect to sign-in
    await expect(page.getByTestId("header-sign-out-button")).not.toBeVisible(); 
  });

  test("keeps the refresh cookie HttpOnly and SameSite=Lax, out of reach for page scripts", async ({
    page,
    context,
  }) => {
    // Arrange
    await page.goto("/events");
    await expectSignedIn(page);

    // Act
    const cookie = (await context.cookies()).find((c) => c.name === "refresh_tk");
    const scriptCookies = await page.evaluate(() => document.cookie);

    // Assert
    expect(cookie?.httpOnly).toBe(true); // XSS cannot read it
    expect(cookie?.sameSite).toBe("Lax"); // not sent with cross-site POSTs (CSRF)
    expect(scriptCookies).not.toContain("refresh_tk");
  });
});
