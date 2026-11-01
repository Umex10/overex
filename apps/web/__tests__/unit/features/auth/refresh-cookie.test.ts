import { describe, expect, it } from "vitest";
import { extractRefreshTk } from "@/features/auth/refresh-cookie";

// --- Tests -----------------------------------------------------------------

describe("extractRefreshTk", () => {
  it("finds the token among several Set-Cookie headers", () => {
    // Act
    const refreshTk = extractRefreshTk([
      "theme=dark; Path=/",
      "refresh_tk=abc.def.ghi; HttpOnly; Path=/; Max-Age=2592000",
    ]);

    // Assert
    expect(refreshTk).toBe("abc.def.ghi");
  });

  it("accepts a single header given as a string", () => {
    // Act
    const refreshTk = extractRefreshTk("refresh_tk=abc.def.ghi; HttpOnly");

    // Assert
    expect(refreshTk).toBe("abc.def.ghi");
  });

  it("keeps '=' inside the value (Base64 padding)", () => {
    // Act
    const refreshTk = extractRefreshTk("refresh_tk=abc==; Path=/");

    // Assert
    expect(refreshTk).toBe("abc==");
  });

  it.each([
    { when: "there is no Set-Cookie header", header: undefined },
    { when: "there is no refresh_tk cookie", header: ["theme=dark"] },
    {
      when: "the refresh_tk cookie is empty",
      header: "refresh_tk=; Max-Age=0",
    },
    // Only starts the same way: must not be mistaken for refresh_tk.
    {
      when: "there is only a refresh_tk_old cookie",
      header: "refresh_tk_old=abc",
    },
  ])("returns null when $when", ({ header }) => {
    // Act
    const refreshTk = extractRefreshTk(header);

    // Assert
    expect(refreshTk).toBeNull();
  });
});
