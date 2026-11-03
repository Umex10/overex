import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { pickFeaturedEvent } from "@/features/event/featured-event";
import type { EventDto } from "@/features/event/event-types";

// --- Test data -------------------------------------------------------------

/** The frozen "now" of every test: the clock never moves on its own. */
const NOW = "2026-06-01T12:00:00Z";

/**
 * An event with the given id and sale start. Every other field is a fixed
 * dummy value: pickFeaturedEvent only looks at `saleStartsAt`.
 */
function makeEvent(id: string, saleStartsAt: string): EventDto {
  return {
    id,
    category: { id: "concert", title: "Concert" },
    title: `Event ${id}`,
    description: "",
    venue: "Stadthalle",
    city: "Wien",
    startsAt: "2026-12-31T20:00:00Z",
    saleStartsAt,
    imageUrl: "",
    status: "PUBLISHED",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  };
}

// --- Setup -----------------------------------------------------------------

beforeEach(() => {
  // Replaces the real clock: Date.now() and new Date() now return NOW.
  vi.useFakeTimers();
  vi.setSystemTime(new Date(NOW));
});

afterEach(() => {
  // Gives the real clock back, so no other test file sees the frozen time.
  vi.useRealTimers();
});

// --- Tests -----------------------------------------------------------------

describe("pickFeaturedEvent", () => {
  it("ignores events whose sale has already started", () => {
    // Arrange
    const saleStarted = makeEvent("may", "2026-05-01T12:00:00Z"); // before NOW
    const saleUpcoming = makeEvent("july", "2026-07-01T12:00:00Z"); // after NOW

    // Act
    const featured = pickFeaturedEvent([saleStarted, saleUpcoming]);

    // Assert
    expect(featured).toBe(saleUpcoming);
  });

  it("returns undefined for an empty list", () => {
    // Act
    const featured = pickFeaturedEvent([]);

    // Assert
    expect(featured).toBeUndefined();
  });

  it("picks the upcoming sale that opens soonest, whatever the list order", () => {
    // Arrange
    const saleUpcomingEarliest = makeEvent("july", "2026-07-01T12:00:00Z");
    const saleUpcomingMiddle = makeEvent("august", "2026-08-01T12:00:00Z");
    const saleUpcomingLatest = makeEvent("september", "2026-09-01T12:00:00Z");

    // Act
    const featured = pickFeaturedEvent([
      saleUpcomingMiddle,
      saleUpcomingEarliest,
      saleUpcomingLatest,
    ]);

    // Assert
    expect(featured).toBe(saleUpcomingEarliest);
  });

  it("falls back to the first event when every sale has started", () => {
    // Arrange: all before NOW; the first one in the list is neither the
    // earliest nor the latest, so only "first in the list" can explain it.
    const saleStartedYesterday = makeEvent("yesterday", "2026-05-31T12:00:00Z");
    const saleStartedLastMonth = makeEvent(
      "last-month",
      "2026-05-01T12:00:00Z",
    );
    const saleStartedLastYear = makeEvent("last-year", "2025-06-01T12:00:00Z");

    // Act
    const featured = pickFeaturedEvent([
      saleStartedLastMonth,
      saleStartedYesterday,
      saleStartedLastYear,
    ]);

    // Assert
    expect(featured).toBe(saleStartedLastMonth);
  });

  it("treats a sale that starts exactly now as already started", () => {
    // Arrange
    const saleStartsNow = makeEvent("now", NOW);
    const saleUpcoming = makeEvent("july", "2026-07-01T12:00:00Z");

    // Act
    const featured = pickFeaturedEvent([saleStartsNow, saleUpcoming]);

    // Assert
    expect(featured).toBe(saleUpcoming);
  });
});
