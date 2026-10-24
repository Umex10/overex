// @vitest-environment node
import { describe, expect, it } from "vitest";
import { NextRequest } from "next/server";
import { proxy } from "@/proxy";

/** Runs the proxy for a path, with or without the refresh cookie. */
async function visit(path: string, withCookie: boolean) {
  const request = new NextRequest(new URL(path, "http://localhost:3000"), {
    headers: withCookie ? { cookie: "refresh_tk=some-token" } : {},
  });
  const response = await proxy(request);
  const location = response.headers.get("location");
  return location ? new URL(location).pathname + new URL(location).search : null;
}

describe("proxy (route gate on the refresh cookie)", () => {
  it("sends visitors without a cookie from protected pages to sign-in", async () => {
    expect(await visit("/events", false)).toBe("/sign-in");
  });

  it("lets visitors without a cookie see the landing and auth pages", async () => {
    expect(await visit("/", false)).toBeNull();
    expect(await visit("/sign-in", false)).toBeNull();
    expect(await visit("/sign-up", false)).toBeNull();
  });

  it("forwards signed-in users from the auth pages to the dashboard", async () => {
    expect(await visit("/sign-in", true)).toBe("/events");
    expect(await visit("/sign-up", true)).toBe("/events");
  });

  it("forwards signed-in users from the landing page with the 'redirected' flag", async () => {
    expect(await visit("/", true)).toBe("/events?redirected=1");
  });

  it("lets signed-in users through to protected pages", async () => {
    expect(await visit("/events", true)).toBeNull();
  });
});
