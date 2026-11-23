import { describe, expect, it } from "vitest";
import {
  combineDateAndTime,
  uncombineDateAndTime,
} from "@/features/event/date-time";

// --- Tests -----------------------------------------------------------------

// All dates are built from local parts (year, month index, day, …), so the
// tests pass in every time zone.
describe("combineDateAndTime", () => {
  it.each([
    { time: "19:30", hours: 19, minutes: 30 },
    { time: "09:05", hours: 9, minutes: 5 },
    { time: "00:00", hours: 0, minutes: 0 },
    { time: "23:59", hours: 23, minutes: 59 },
  ])("joins the day with $time", ({ time, hours, minutes }) => {
    // Arrange
    const day = new Date(2026, 5, 20);

    // Act
    const combined = combineDateAndTime(day, time);

    // Assert
    expect(combined).toEqual(new Date(2026, 5, 20, hours, minutes));
  });

  it("replaces the time the day already had, including the seconds", () => {
    // Arrange
    const day = new Date(2026, 5, 20, 8, 15, 42, 123);

    // Act
    const combined = combineDateAndTime(day, "19:30");

    // Assert
    expect(combined).toEqual(new Date(2026, 5, 20, 19, 30, 0, 0));
  });

  it("does not change the given day", () => {
    // Arrange
    const day = new Date(2026, 5, 20);

    // Act
    combineDateAndTime(day, "19:30");

    // Assert
    expect(day).toEqual(new Date(2026, 5, 20));
  });
});

describe("uncombineDateAndTime", () => {
  it("splits a Date into the day at midnight and the time", () => {
    // Arrange
    const value = new Date(2026, 5, 20, 19, 30, 42);

    // Act
    const result = uncombineDateAndTime(value);

    // Assert
    expect(result).toEqual({ date: new Date(2026, 5, 20), time: "19:30" });
  });

  it("splits an ISO string from the backend", () => {
    // Arrange
    const value = new Date(2026, 5, 20, 19, 30).toISOString();

    // Act
    const result = uncombineDateAndTime(value);

    // Assert
    expect(result).toEqual({ date: new Date(2026, 5, 20), time: "19:30" });
  });

  it.each([
    { time: "09:05", value: new Date(2026, 5, 20, 9, 5) },
    { time: "00:00", value: new Date(2026, 5, 20, 0, 0) },
  ])("fills hours and minutes up to two digits: $time", ({ time, value }) => {
    // Act
    const result = uncombineDateAndTime(value);

    // Assert
    expect(result.time).toBe(time);
  });

  it("does not change the given Date", () => {
    // Arrange
    const value = new Date(2026, 5, 20, 19, 30);

    // Act
    uncombineDateAndTime(value);

    // Assert
    expect(value).toEqual(new Date(2026, 5, 20, 19, 30));
  });

  it("is reversed by combineDateAndTime", () => {
    // Arrange
    const value = new Date(2026, 5, 20, 19, 30);
    const { date, time } = uncombineDateAndTime(value);

    // Act
    const combined = combineDateAndTime(date, time);

    // Assert
    expect(combined).toEqual(value);
  });
});
