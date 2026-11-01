import {
  deleteRefreshCookie,
  getAccessTkAction,
  signInAction,
  signUpAction,
} from "@/features/auth/auth-actions";
import {
  REFRESH_COOKIE,
  REFRESH_COOKIE_MAX_AGE,
} from "@/features/auth/refresh-cookie";
import { beforeEach, describe, expect, it, vi } from "vitest";

// --- Mocks -----------------------------------------------------------------

const post = vi.fn();
const get = vi.fn();

const cookieStore = {
  get: vi.fn(),
  set: vi.fn(),
  delete: vi.fn(),
};

vi.mock("axios", async (importOriginal) => {
  const actual = await importOriginal<typeof import("axios")>();
  return {
    ...actual,
    default: {
      ...actual.default,
      create: () => ({
        post: (...args: unknown[]) => post(...args),
        get: (...args: unknown[]) => get(...args),
      }),
    },
  };
});

vi.mock("next/headers", () => ({
  cookies: async () => cookieStore,
}));

// --- Test data -------------------------------------------------------------

const signUpForm = {
  name: "Max_Mustermann",
  email: "max@mail.com",
  phone: "+43 555 555555",
  password: "Max+12345",
  terms: true,
};

const signUpRequestBody = {
  name: "Max_Mustermann",
  email: "max@mail.com",
  phone: "+43 555 555555",
  password: "Max+12345",
};

const signInForm = {
  email: "max@mail.com",
  password: "Max+12345",
};

const authResponse = { accessTk: "tk", expiresIn: 900 };

function expectedRefreshCookie(value: string) {
  return {
    name: REFRESH_COOKIE,
    value,
    httpOnly: true,
    secure: false,
    path: "/",
    maxAge: REFRESH_COOKIE_MAX_AGE,
    sameSite: "lax",
  };
}

// --- Arrange helpers -------------------------------------------------------

function backendRespondsWithAuthResponse(refreshTkValue: string) {
  const response = {
    data: authResponse,
    headers: { "set-cookie": [`refresh_tk=${refreshTkValue}; HttpOnly`] },
  };

  post.mockResolvedValue(response);

  get.mockResolvedValue(response);
}

function backendRespondsWith401() {
  const response = {
    isAxiosError: true,
    response: { status: 401, data: "Axios Error" },
  };

  post.mockRejectedValue(response);
  get.mockRejectedValue(response);
}

function backendIsDown() {
  const response = {
    isAxiosError: true,
    response: undefined,
  };

  post.mockRejectedValue(response);
  get.mockRejectedValue(response);
}

function userHasRefreshCookie(refreshTkValue: string) {
  cookieStore.get.mockReturnValue({
    name: REFRESH_COOKIE,
    value: refreshTkValue,
  });
}

function userHasNoRefreshCookie() {
  cookieStore.get.mockReturnValue(undefined);
}

// --- Setup -----------------------------------------------------------------

beforeEach(() => {
  for (let mock of [
    post,
    get,
    cookieStore.get,
    cookieStore.set,
    cookieStore.delete,
  ]) {
    mock.mockReset();
  }
});

// --- Tests -----------------------------------------------------------------

describe("auth-actions", () => {
  describe.each([
    {
      action: "signUpAction",
      call: () => signUpAction(signUpForm),
      route: "sign-up",
      body: signUpRequestBody,
    },
    {
      action: "signInAction",
      call: () => signInAction(signInForm),
      route: "sign-in",
      body: signInForm,
    },
  ])("$action", ({ call, route, body }) => {
    it("returns the auth response and stores the refresh cookie", async () => {
      // Arrange
      const refreshTkValue = "abc";

      backendRespondsWithAuthResponse(refreshTkValue);

      // Act
      const res = await call();

      // Assert
      expect(res).toEqual({
        success: true,
        data: authResponse,
      });

      expect(post).toHaveBeenCalledWith(route, body);

      expect(cookieStore.set).toHaveBeenLastCalledWith(
        expectedRefreshCookie(refreshTkValue),
      );
    });

    it("returns the auth response but sets no cookie when the refresh token is empty", async () => {
      // Arrange
      const refreshTkValue = ""; // doesnt trigger cookieStore

      backendRespondsWithAuthResponse(refreshTkValue);

      // Act
      const res = await call();

      // Assert
      expect(res).toEqual({
        success: true,
        data: authResponse,
      });

      expect(cookieStore.set).not.toHaveBeenCalled();
    });

    it("returns the error and sets no cookie when the backend responds with 401", async () => {
      // Arrange
      backendRespondsWith401();

      // Act
      const res = await call();

      // Assert
      expect(res).toEqual({
        success: false,
        error: {
          message: expect.any(String),
          status: 401,
        },
      });

      expect(cookieStore.set).not.toHaveBeenCalled();
    });

    it("returns 503 and sets no cookie when the backend is not reachable", async () => {
      // Arrange
      backendIsDown();

      // Act
      const res = await call();

      // Assert
      expect(res).toEqual({
        success: false,
        error: {
          message: expect.any(String),
          status: 503,
        },
      });

      expect(cookieStore.set).not.toHaveBeenCalled();
    });
  });

  describe("deleteRefreshCookie", () => {
    it("deletes the refresh cookie", async () => {
      // Act
      await deleteRefreshCookie();

      // Assert
      expect(cookieStore.delete).toHaveBeenLastCalledWith(REFRESH_COOKIE);
    });
  });

  describe("getAccessTkAction", () => {
    it("reads the refresh cookie and returns the new access token", async () => {
      // Arrange
      const refreshTkValue = "abc";

      userHasRefreshCookie(refreshTkValue);

      backendRespondsWithAuthResponse(refreshTkValue);

      // Act
      const res = await getAccessTkAction();

      // Assert
      expect(cookieStore.get).toHaveBeenLastCalledWith(REFRESH_COOKIE);

      expect(res).toEqual({
        success: true,
        data: authResponse,
      });
    });

    it("returns null when there is no refresh cookie (guest mode)", async () => {
      // Arrange
      userHasNoRefreshCookie();

      backendRespondsWithAuthResponse("abc");

      // Act
      const res = await getAccessTkAction();

      // Assert
      expect(res).toEqual({
        success: true,
        data: null,
      });
    });

    it("returns the error when the backend responds with 401", async () => {
      // Arrange
      userHasRefreshCookie("abc");

      backendRespondsWith401();

      // Act
      const res = await getAccessTkAction();

      // Assert
      expect(res).toEqual({
        success: false,
        error: {
          message: expect.any(String),
          status: 401,
        },
      });
    });

    it("returns 503 when the backend is not reachable", async () => {
      // Arrange
      userHasRefreshCookie("abc");

      backendIsDown();

      // Act
      const res = await getAccessTkAction();

      // Assert
      expect(res).toEqual({
        success: false,
        error: {
          message: expect.any(String),
          status: 503,
        },
      });
    });
  });
});
