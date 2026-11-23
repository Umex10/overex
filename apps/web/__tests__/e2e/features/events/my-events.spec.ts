import { test, expect } from "@playwright/test";
import {
  changeStatus,
  createDraft,
  signInAsNewOrganizer,
} from "../../utils/my-events";
import { openMyEvent, pickDay } from "../../utils/flows";
import { seedCategories } from "../../utils/seed";

/**
 * Every test starts signed out and signs in as its own new user, so it only
 * sees the events it has created itself and never disturbs another test.
 */

// --- Tests -----------------------------------------------------------------

test.describe("my events", () => {
  for (const path of ["/my-events", "/create-event"]) {
    test(`redirects a guest from ${path} to sign-in`, async ({ page }) => {
      // Act
      await page.goto(path);

      // Assert
      await expect(page).toHaveURL("/sign-in");
    });
  }

  test.describe("list", () => {
    test("shows only the user's own events, earliest first", async ({
      page,
    }) => {
      // Arrange: created in the wrong order; the seed events belong to someone else
      const organizer = await signInAsNewOrganizer(page);
      await createDraft(organizer, "Later Event", 60);
      await createDraft(organizer, "Earlier Event", 50);

      // Act
      await page.goto("/my-events");

      // Assert
      const items = page.getByTestId("my-event-item");
      await expect(items).toHaveCount(2);
      await expect(items).toContainText(["Earlier Event", "Later Event"]);
    });

    test("shows a hint when the user has no events", async ({ page }) => {
      // Arrange
      await signInAsNewOrganizer(page);

      // Act
      await page.goto("/my-events");

      // Assert
      await expect(page.getByRole("tab", { name: "Alle (0)" })).toBeVisible();
      await expect(page.getByText("Keine Events gefunden.")).toBeVisible();
    });

    // The same event list for every tab, only the expected part differs.
    // "Beendet" stays empty: the API can't create an event in the past.
    for (const { tab, titles } of [
      { tab: "Alle (3)", titles: ["My Draft", "My Published", "My Canceled"] },
      { tab: "Entwürfe (1)", titles: ["My Draft"] },
      { tab: "Veröffentlicht (1)", titles: ["My Published"] },
      { tab: "Abgesagt (1)", titles: ["My Canceled"] },
      { tab: "Beendet (0)", titles: [] },
    ]) {
      test(`shows the matching events in the tab ${tab}`, async ({ page }) => {
        // Arrange: changeStatus moves the start to day 100, the draft is earlier
        const organizer = await signInAsNewOrganizer(page);
        await createDraft(organizer, "My Draft", 50);
        const published = await createDraft(organizer, "My Published");
        await changeStatus(organizer, published, "PUBLISHED");
        const canceled = await createDraft(organizer, "My Canceled");
        await changeStatus(
          organizer,
          await changeStatus(organizer, canceled, "PUBLISHED"),
          "CANCELED",
        );
        await page.goto("/my-events");

        // Act
        await page.getByRole("tab", { name: tab }).click();

        // Assert
        const items = page.getByTestId("my-event-item");
        await expect(items).toHaveCount(titles.length);
        await expect(items).toContainText(titles);
      });
    }

    test("shows only the events of the selected category", async ({ page }) => {
      // Arrange
      const organizer = await signInAsNewOrganizer(page);
      await createDraft(organizer, "My Concert", 50, seedCategories.concert.id);
      await createDraft(organizer, "My Tennis", 60, seedCategories.tennis.id);
      await page.goto("/my-events");

      // Act
      await page
        .getByRole("radio", { name: seedCategories.tennis.title })
        .click();

      // Assert
      const items = page.getByTestId("my-event-item");
      await expect(items).toHaveCount(1);
      await expect(items).toContainText(["My Tennis"]);
    });

    test("shows the latest events first after switching the sort order", async ({
      page,
    }) => {
      // Arrange
      const organizer = await signInAsNewOrganizer(page);
      await createDraft(organizer, "Earlier Event", 50);
      await createDraft(organizer, "Later Event", 60);
      await page.goto("/my-events");

      // Act
      await page.getByRole("button", { name: "Earliest first" }).click();

      // Assert
      await expect(page.getByTestId("my-event-item")).toContainText([
        "Later Event",
        "Earlier Event",
      ]);
    });

    test("goes back to the first page when the tab changes", async ({
      page,
    }) => {
      // Arrange: 11 drafts = 2 pages, the second one is open
      const organizer = await signInAsNewOrganizer(page);
      for (let day = 1; day <= 11; day++) {
        await createDraft(organizer, `Draft ${day}`, 50 + day);
      }
      await page.goto("/my-events");
      await page.getByRole("link", { name: "Go to next page" }).click();
      const items = page.getByTestId("my-event-item");
      await expect(items).toHaveCount(1);

      // Act
      await page.getByRole("tab", { name: "Entwürfe (11)" }).click();

      // Assert
      await expect(items).toHaveCount(10);
    });
  });

  test.describe("editor", () => {
    test("opens the clicked event with its values", async ({ page }) => {
      // Arrange
      const organizer = await signInAsNewOrganizer(page);
      const event = await createDraft(organizer, "My Draft");

      // Act
      await openMyEvent(page, event);

      // Assert
      await expect(page.locator("#title")).toHaveValue("My Draft");
      await expect(page.locator("#venue")).toHaveValue("Donauinsel");
      await expect(page.locator("#status")).toContainText("DRAFT");
      await expect(page.getByRole("button", { name: "Save" })).toBeDisabled();
    });

    test("saves a changed title", async ({ page }) => {
      // Arrange
      const organizer = await signInAsNewOrganizer(page);
      const event = await createDraft(organizer, "Old Title");
      await openMyEvent(page, event);

      // Act
      await page.locator("#title").fill("New Title");
      await page.getByRole("button", { name: "Save" }).click();

      // Assert
      await expect(page.locator(".toast-success")).toBeVisible();
      await expect(page).toHaveURL(`/events/${event.id}`);
      await expect(
        page.getByRole("heading", { name: "New Title" }),
      ).toBeVisible();
    });

    test("publishes a draft", async ({ page }) => {
      // Arrange
      const organizer = await signInAsNewOrganizer(page);
      const event = await createDraft(organizer, "My Draft");
      await openMyEvent(page, event);

      // Act
      await page.locator("#status").click();
      await page.getByRole("option", { name: "PUBLISHED" }).click();
      await page.getByRole("button", { name: "Save" }).click();

      // Assert
      await expect(page).toHaveURL(`/events/${event.id}`);
      await page.goto("/my-events");
      await expect(
        page.getByRole("tab", { name: "Veröffentlicht (1)" }),
      ).toBeVisible();
    });

    test("does not offer to cancel a draft", async ({ page }) => {
      // Arrange
      const organizer = await signInAsNewOrganizer(page);
      const event = await createDraft(organizer, "My Draft");
      await openMyEvent(page, event);

      // Act
      await page.locator("#status").click();

      // Assert
      await expect(
        page.getByRole("option", { name: "CANCELED" }),
      ).toBeDisabled();
    });

    test("shows an error when the event was changed in the meantime", async ({
      page,
    }) => {
      // Arrange: the editor has loaded version 0, then the event becomes version 1
      const organizer = await signInAsNewOrganizer(page);
      const event = await createDraft(organizer, "My Draft");
      await openMyEvent(page, event);
      await expect(page.locator("#title")).toHaveValue("My Draft");
      await changeStatus(organizer, event, "PUBLISHED");

      // Act
      await page.locator("#title").fill("Outdated Title");
      await page.getByRole("button", { name: "Save" }).click();

      // Assert
      await expect(page.locator(".toast-error")).toContainText(
        "changed in the meantime",
      );
      await expect(page).toHaveURL(`/my-events/${event.id}`);
    });

    test("creates a draft that then shows up in the list", async ({ page }) => {
      // Arrange
      await signInAsNewOrganizer(page);
      await page.goto("/create-event");

      // Act
      await page.locator("#title").fill("Brand-new Event");
      await page.locator("#category").click();
      await page
        .getByRole("option", { name: seedCategories.concert.title })
        .click();
      await page.locator("#description").fill("An evening to remember.");
      await page
        .locator("#image-url")
        .fill("https://images.unsplash.com/photo-1501386761578-eac5c94b800a");
      await pickDay(page, "#start-date", 15);
      await page.locator("#start-time").fill("20:00");
      await page.locator("#venue").fill("Gasometer");
      await page.locator("#city").fill("Wien");
      await pickDay(page, "#sale-date", 10);
      await page.locator("#sale-time").fill("10:00");
      await page.getByRole("button", { name: "Create" }).click();

      // Assert
      await expect(page.locator(".toast-success")).toBeVisible();
      await page.goto("/my-events");
      await page.getByRole("tab", { name: "Entwürfe (1)" }).click();
      await expect(page.getByTestId("my-event-item")).toContainText([
        "Brand-new Event",
      ]);
    });

    test("shows the field error and sends nothing when the title is empty", async ({
      page,
    }) => {
      // Arrange
      const organizer = await signInAsNewOrganizer(page);
      const event = await createDraft(organizer, "My Draft");
      await openMyEvent(page, event);

      // Act
      await page.locator("#title").fill("");
      await page.getByRole("button", { name: "Save" }).click();

      // Assert
      await expect(page.getByText("Title is required.")).toBeVisible();
      await expect(page).toHaveURL(`/my-events/${event.id}`);
    });
  });
});
