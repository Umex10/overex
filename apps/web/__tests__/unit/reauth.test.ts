// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from "vitest";

// The server action is replaced: it would need a Next.js request context.
const getAccessTkAction = vi.fn();
vi.mock("@/features/auth/auth-actions", () => ({
  getAccessTkAction: () => getAccessTkAction(),
  signInAction: vi.fn(),
  signUpAction: vi.fn(),
}));

const { makeStore } = await import("@/redux/store");
const { apiSlice } = await import("@/redux/api-slice");
await import("@/features/auth/auth-api"); // injects getAccessTk
await import("@/features/user/user-api"); // injects getMe

/** Endpoints are injected at runtime, so the test looks them up untyped. */
const endpoints = apiSlice.endpoints as unknown as {
  getMe: { initiate: () => Parameters<ReturnType<typeof makeStore>["dispatch"]>[0] };
};

/** A store whose cached access token has expired meanwhile. */
async function storeWithExpiredToken() {
  const store = makeStore();
  // upsert is async: wait until the token is really in the cache.
  await store.dispatch(
    apiSlice.util.upsertQueryData("getAccessTk" as never, undefined as never, { accessTk: "expired" } as never),
  );
  return store;
}

/** Fake backend for GET /me: only the token "fresh" is accepted. */
function mockBackend() {
  const seenTokens: (string | null)[] = [];
  globalThis.fetch = vi.fn(async (input: RequestInfo | URL) => {
    const request = input as Request;
    const auth = request.headers.get("authorization");
    seenTokens.push(auth);
    return auth === "Bearer fresh"
      ? new Response(JSON.stringify({ name: "Max" }), { status: 200, headers: { "content-type": "application/json" } })
      : new Response("{}", { status: 401, headers: { "content-type": "application/json" } });
  }) as typeof fetch;
  return seenTokens;
}

describe("automatic access-token renewal", () => {
  beforeEach(() => getAccessTkAction.mockReset());

  it("renews an expired access token once and repeats the request", async () => {
    const store = await storeWithExpiredToken();
    const seenTokens = mockBackend();
    getAccessTkAction.mockResolvedValue({ success: true, data: { accessTk: "fresh", expiresIn: 900 } });

    const result = await store.dispatch(endpoints.getMe.initiate());

    expect((result as { data?: unknown }).data).toEqual({ name: "Max" });
    expect(seenTokens).toEqual(["Bearer expired", "Bearer fresh"]);
    expect(getAccessTkAction).toHaveBeenCalledTimes(1);
  });

  it("gives up when the refresh cookie is rejected too", async () => {
    const store = await storeWithExpiredToken();
    const seenTokens = mockBackend();
    getAccessTkAction.mockResolvedValue({ success: false, error: { status: 401, message: "Invalid refresh token" } });

    const result = await store.dispatch(endpoints.getMe.initiate());

    expect((result as { error?: { status?: number } }).error?.status).toBe(401);
    expect(seenTokens).toEqual(["Bearer expired"]); // no second attempt without a new token
  });
});
