import { test, expect, type Page } from "@playwright/test";
import { openEventDetails } from "../../utils/flows";
import { seedCategories, seedEvents } from "../../utils/seed";

// --- Test data -------------------------------------------------------------

// What the list shows for the seed (apps/backend/.../db/e2e-seed.sql):
// 12 events, 10 per page.

/** Page 1, sorted by start date, earliest first (the default). */
const firstPageEarliestFirst = [
  seedEvents.jazzNight.title,
  seedEvents.rockClassics.title,
  seedEvents.summerBeats.title,
  seedEvents.pianoEvening.title,
  seedEvents.indieShowcase.title,
  seedEvents.electroOpenAir.title,
  seedEvents.acousticSessions.title,
  seedEvents.symphonyNo9.title,
  seedEvents.lakeFestival.title,
  seedEvents.hipHopLive.title,
];

/** Page 2, earliest first: the 2 events that are left. */
const secondPageEarliestFirst = [
  seedEvents.bluesBrothersTribute.title,
  seedEvents.winterFestival.title,
];

/** Page 1, latest first: event 12 down to 03. */
const firstPageLatestFirst = [
  seedEvents.winterFestival.title,
  seedEvents.bluesBrothersTribute.title,
  seedEvents.hipHopLive.title,
  seedEvents.lakeFestival.title,
  seedEvents.symphonyNo9.title,
  seedEvents.acousticSessions.title,
  seedEvents.electroOpenAir.title,
  seedEvents.indieShowcase.title,
  seedEvents.pianoEvening.title,
  seedEvents.summerBeats.title,
];

/** All 8 concerts, earliest first: they fit on one page. */
const concertsEarliestFirst = [
  seedEvents.jazzNight.title,
  seedEvents.rockClassics.title,
  seedEvents.pianoEvening.title,
  seedEvents.indieShowcase.title,
  seedEvents.acousticSessions.title,
  seedEvents.symphonyNo9.title,
  seedEvents.hipHopLive.title,
  seedEvents.bluesBrothersTribute.title,
];

/** A valid UUID that no event in the seed has. */
const unknownEventId = "10000000-0000-0000-0000-000000000099";

// --- Tests -----------------------------------------------------------------

test.describe("events", () => {
  test.describe("list", () => {
    test("shows the first page of events, earliest first", async ({ page }) => {
      // Act
      await page.goto("/events");

      // Assert
      const eventCards = page.getByTestId("event-card");
      await expect(eventCards).toHaveCount(10);
      await expect(eventCards).toContainText(firstPageEarliestFirst);
    });
  });

  test.describe("details", () => {
    test("opens the details of the clicked event", async ({ page }) => {
      // Arrange
      await page.goto("/events");

      // Act
      await page
        .getByTestId("event-card")
        .filter({ hasText: seedEvents.jazzNight.title })
        .click();

      // Assert
      await expect(page).toHaveURL(`/events/${seedEvents.jazzNight.id}`);
      await expect(
        page.getByRole("heading", { name: seedEvents.jazzNight.title }),
      ).toBeVisible();
    });

    for (const { via, goBack } of [
      {
        via: "the browser's back button",
        goBack: (page: Page) => page.goBack(),
      },
      {
        via: "the breadcrumb",
        goBack: (page: Page) =>
          page
            .getByRole("navigation", { name: "breadcrumb" })
            .getByRole("link", { name: "Events" })
            .click(),
      },
    ]) {
      test(`returns from the details to the event list via ${via}`, async ({
        page,
      }) => {
        // Arrange
        await openEventDetails(page, seedEvents.jazzNight);

        // Act
        await goBack(page);

        // Assert
        await expect(page).toHaveURL("/events");
        await expect(page.getByTestId("event-card")).toHaveCount(10);
      });
    }

    test("tells the user when the event does not exist", async ({ page }) => {
      // Act
      await page.goto(`/events/${unknownEventId}`);

      // Assert
      await expect(
        page.getByText(
          `The Event with the id: ${unknownEventId} doesn't exist.`,
        ),
      ).toBeVisible();
    });
  });

  test.describe("featured event", () => {
    test("features the event whose sale opens next and links to it", async ({
      page,
    }) => {
      // Arrange
      await page.goto("/events");
      const featuredEvent = page.getByTestId("featured-event");

      // Assert: Acoustic Sessions' sale opens in 2 days, sooner than any other
      await expect(
        featuredEvent.getByRole("heading", {
          name: seedEvents.acousticSessions.title,
        }),
      ).toBeVisible();

      // Act
      await featuredEvent.click();

      // Assert
      await expect(page).toHaveURL(`/events/${seedEvents.acousticSessions.id}`);
    });
  });

  test.describe("category filter", () => {
    test("shows only the events of the selected category", async ({ page }) => {
      // Arrange
      await page.goto("/events");

      // Act
      await page
        .getByRole("radio", { name: seedCategories.concert.title })
        .click();

      // Assert
      await expect(
        page.getByRole("radio", {
          name: seedCategories.concert.title,
          checked: true,
        }),
      ).toBeVisible();
      const eventCards = page.getByTestId("event-card");
      await expect(eventCards).toHaveCount(8);
      await expect(eventCards).toContainText(concertsEarliestFirst);
    });

    test("shows a hint and keeps the filters when a category has no events", async ({
      page,
    }) => {
      // Arrange
      await page.goto("/events");

      // Act
      await page
        .getByRole("radio", { name: seedCategories.tennis.title })
        .click();

      // Assert
      await expect(page.getByTestId("event-card")).toHaveCount(0);
      await expect(page.getByText("Keine Events gefunden.")).toBeVisible();
      // The user can switch back: the filter is still there.
      await expect(page.getByRole("radio", { name: "All" })).toBeVisible();
    });
  });

  test.describe("sort order", () => {
    test("shows the latest events first after switching the sort order", async ({
      page,
    }) => {
      // Arrange
      await page.goto("/events");

      // Act
      await page.getByRole("button", { name: "Earliest first" }).click();

      // Assert: the button now names the current order
      await expect(
        page.getByRole("button", { name: "Latest first" }),
      ).toBeVisible();
      const eventCards = page.getByTestId("event-card");
      await expect(eventCards).toHaveCount(10);
      await expect(eventCards).toContainText(firstPageLatestFirst);
    });
  });

  test.describe("pagination", () => {
    test("shows the remaining events on the next page", async ({ page }) => {
      // Arrange
      await page.goto("/events");

      // Act
      await page.getByRole("link", { name: "Go to next page" }).click();

      // Assert
      const eventCards = page.getByTestId("event-card");
      await expect(eventCards).toHaveCount(2);
      await expect(eventCards).toContainText(secondPageEarliestFirst);
    });

    test("shows the first page again after going back a page", async ({
      page,
    }) => {
      // Arrange: on page 2
      await page.goto("/events");
      await page.getByRole("link", { name: "Go to next page" }).click();
      const eventCards = page.getByTestId("event-card");
      await expect(eventCards).toHaveCount(2);

      // Act
      await page.getByRole("link", { name: "Go to previous page" }).click();

      // Assert
      await expect(eventCards).toHaveCount(10);
      await expect(eventCards).toContainText(firstPageEarliestFirst);
    });
  });
});
