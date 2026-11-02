/**
 * Registers the bootstrap account and saves the browser's storage state (the
 * HTTP-only refresh cookie). The `features` project reuses that state, so its
 * signed-in specs skip the sign-up.
 */
import { test as setup } from "@playwright/test";
import { bootstrapAccount } from "../utils/accounts";
import { expectSignedIn, openSignUp, submitSignUp } from "../utils/flows";

setup("registers the bootstrap account and saves its session", async ({ page }) => {
  // Act
  await openSignUp(page);
  await submitSignUp(page, bootstrapAccount);

  // Assert
  await expectSignedIn(page);

  await page.context().storageState({ path: "playwright/.auth/chromium.json" });
});
