// @vitest-environment node
import { describe, expect, it } from "vitest";
import { NextRequest } from "next/server";
import { proxy } from "@/proxy";

// --- Helpers ---------------------------------------------------------------

/**
 * Runs the proxy for a path, with or without the refresh cookie, and returns
 * where it redirects to (path + query), or null when it lets the request pass.
 */
async function visit(path: string, withCookie: boolean) {
  const request = new NextRequest(new URL(path, "http://localhost:3000"), {
    headers: withCookie ? { cookie: "refresh_tk=some-token" } : {},
  });
  const response = await proxy(request);
  const location = response.headers.get("location");
  return location
    ? new URL(location).pathname + new URL(location).search
    : null;
}

// --- Tests -----------------------------------------------------------------

describe("proxy", () => {
  it("sends visitors without a cookie from protected pages to sign-in", async () => {
    // Act
    const redirect = await visit("/my-tickets", false);

    // Assert
    expect(redirect).toBe("/sign-in");
  });

  it("does not treat paths that only start like /events as guest pages", async () => {
    // Act
    const redirect = await visit("/events-archive", false);

    // Assert
    expect(redirect).toBe("/sign-in");
  });

  it.each(["/events", "/events/some-id"])(
    "lets visitors without a cookie browse %s (guest mode)",
    async (path) => {
      // Act
      const redirect = await visit(path, false);

      // Assert
      expect(redirect).toBeNull();
    },
  );

  it.each(["/", "/sign-in", "/sign-up"])(
    "lets visitors without a cookie see %s",
    async (path) => {
      // Act
      const redirect = await visit(path, false);

      // Assert
      expect(redirect).toBeNull();
    },
  );

  it.each(["/sign-in", "/sign-up"])(
    "forwards signed-in users from %s to the dashboard",
    async (path) => {
      // Act
      const redirect = await visit(path, true);

      // Assert
      expect(redirect).toBe("/events");
    },
  );

  it("forwards signed-in users from the landing page with the 'redirected' flag", async () => {
    // Act
    const redirect = await visit("/", true);

    // Assert
    expect(redirect).toBe("/events?redirected=1");
  });

  it("lets signed-in users browse the events", async () => {
    // Act
    const redirect = await visit("/events", true);

    // Assert
    expect(redirect).toBeNull();
  });

  it("lets signed-in users through to protected pages", async () => {
    // Act
    const redirect = await visit("/my-tickets", true);

    // Assert
    expect(redirect).toBeNull();
  });
});
