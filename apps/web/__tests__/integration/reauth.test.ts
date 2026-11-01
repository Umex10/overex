// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AppStore } from "@/redux/store";

// --- Mocks -----------------------------------------------------------------

// The server action is replaced: it would need a Next.js request context.
const getAccessTkAction = vi.fn();
vi.mock("@/features/auth/auth-actions", () => ({
  getAccessTkAction: (...args: unknown[]) => getAccessTkAction(...args),
  signInAction: vi.fn(),
  signUpAction: vi.fn(),
}));

const { makeStore } = await import("@/redux/store");
const { authApi } = await import("@/features/auth/auth-api"); // injects getAccessTk
const { meApi } = await import("@/features/user/user-api"); // injects getMe

// --- Arrange helpers -------------------------------------------------------

/** Puts the given access token into the store's cache, as a sign-in would. */
async function storeHasAccessTk(accessTk: string) {
  // upsert is async: wait until the token is really in the cache.
  await store.dispatch(
    authApi.util.upsertQueryData("getAccessTk", undefined, {
      accessTk,
      expiresIn: 900,
    }),
  );
}

/** Fake backend for GET /me: only the token "fresh" is accepted. */
function backendAcceptsOnlyFreshToken() {
  const seenTokens: (string | null)[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL) => {
      const request = input as Request;
      const auth = request.headers.get("authorization");
      seenTokens.push(auth);
      return auth === "Bearer fresh"
        ? new Response(JSON.stringify({ name: "Max" }), {
            status: 200,
            headers: { "content-type": "application/json" },
          })
        : new Response("{}", {
            status: 401,
            headers: { "content-type": "application/json" },
          });
    }),
  );
  return seenTokens;
}

// --- Setup -----------------------------------------------------------------

let store: AppStore;

beforeEach(() => {
  store = makeStore();
  vi.resetAllMocks();
});

// --- Tests -----------------------------------------------------------------

describe("baseQueryWithReauth", () => {
  it("sends the cached token and renews nothing when the request succeeds", async () => {
    // Arrange
    await storeHasAccessTk("fresh");
    const seenTokens = backendAcceptsOnlyFreshToken();

    // Act
    const result = await store.dispatch(meApi.endpoints.getMe.initiate());

    // Assert
    expect(result.data).toEqual({ name: "Max" });
    expect(seenTokens).toEqual(["Bearer fresh"]);
    expect(getAccessTkAction).not.toHaveBeenCalled();
  });

  it("renews a rejected token once and repeats the request with the new one", async () => {
    // Arrange
    await storeHasAccessTk("expired");
    const seenTokens = backendAcceptsOnlyFreshToken();
    getAccessTkAction.mockResolvedValue({
      success: true,
      data: { accessTk: "fresh", expiresIn: 900 },
    });

    // Act
    const result = await store.dispatch(meApi.endpoints.getMe.initiate());

    // Assert
    expect(result.data).toEqual({ name: "Max" });
    expect(seenTokens).toEqual(["Bearer expired", "Bearer fresh"]);
    expect(getAccessTkAction).toHaveBeenCalledTimes(1);
  });

  it("gives up without a second request when the renewal fails too", async () => {
    // Arrange
    await storeHasAccessTk("expired");
    const seenTokens = backendAcceptsOnlyFreshToken();
    getAccessTkAction.mockResolvedValue({
      success: false,
      error: { status: 401, message: "Invalid refresh token" },
    });

    // Act
    const result = await store.dispatch(meApi.endpoints.getMe.initiate());

    // Assert
    expect(result.error).toMatchObject({ status: 401 });
    expect(seenTokens).toEqual(["Bearer expired"]); // no second attempt without a new token
  });

  it("sends no Authorization header without a token, then retries with a renewed one", async () => {
    // Arrange: "" is falsy, so the first request goes out without a header
    await storeHasAccessTk("");
    const seenTokens = backendAcceptsOnlyFreshToken();
    getAccessTkAction.mockResolvedValue({
      success: true,
      data: { accessTk: "fresh", expiresIn: 900 },
    });

    // Act
    const result = await store.dispatch(meApi.endpoints.getMe.initiate());

    // Assert
    expect(result.data).toEqual({ name: "Max" });
    expect(seenTokens).toEqual([null, "Bearer fresh"]);
    expect(getAccessTkAction).toHaveBeenCalledTimes(1);
  });
});
