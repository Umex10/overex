import { test, expect } from "@playwright/test";
import { bootstrapAccount } from "./utils/accounts";
import { expectEventFor, signOut } from "./utils/flows";

/**
 * Specs that start signed in as the bootstrap account (storage state from
 * auth.setup.ts). Every test gets its own browser context, so signing out in
 * one test does not affect the others.
 */
test.describe("Signed in", () => {
  test("reloading the dashboard keeps the session (silent refresh)", async ({ page }) => {
    // A full page load starts with an empty Redux cache: the access token is
    // fetched again with the refresh cookie before GET /me can succeed.
    await page.goto("/event");
    await expectEventFor(page, bootstrapAccount);

    await page.reload();
    await expectEventFor(page, bootstrapAccount);
  });

  test("landing and auth pages forward to event", async ({ page }) => {
    await page.goto("/sign-in");
    await expect(page).toHaveURL("/event");

    await page.goto("/sign-up");
    await expect(page).toHaveURL("/event");

    await page.goto("/");
    await expect(page).toHaveURL("/event?redirected=1");
  });

  // test("signing out ends the session", async ({ page, context }) => {
  //   await page.goto("/event");
  //   await expectEventFor(page, bootstrapAccount);

  //   await signOut(page);

  //   expect((await context.cookies()).some((c) => c.name === "refresh_tk")).toBe(false);
  //   await page.goto("/event");
  //   await expect(page).toHaveURL("/sign-in");
  // });

  test("the refresh token is out of reach for page scripts", async ({ page, context }) => {
    await page.goto("/event");
    await expectEventFor(page, bootstrapAccount);

    const cookie = (await context.cookies()).find((c) => c.name === "refresh_tk");
    expect(cookie?.httpOnly).toBe(true); // XSS cannot read it
    expect(cookie?.sameSite).toBe("Lax"); // not sent with cross-site POSTs (CSRF)
    expect(await page.evaluate(() => document.cookie)).not.toContain("refresh_tk");
  });
});
