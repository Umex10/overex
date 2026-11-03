import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AuthProvider from "@/features/auth/components/auth-provider";
import type { User } from "@/features/user/user-types";

// --- Mocks -----------------------------------------------------------------

const useGetAccessTkQuery = vi.fn();
const useGetMeQuery = vi.fn();
const deleteRefreshCookie = vi.fn();
const replace = vi.fn();

// The wrappers pass every argument on, so the mocks see the real call
// (e.g. `{ skip: true }` for useGetMeQuery).
vi.mock("@/features/auth/auth-api", () => ({
  useGetAccessTkQuery: (...args: unknown[]) => useGetAccessTkQuery(...args),
}));

vi.mock("@/features/user/user-api", () => ({
  useGetMeQuery: (...args: unknown[]) => useGetMeQuery(...args),
}));

vi.mock("@/features/auth/auth-actions", () => ({
  deleteRefreshCookie: (...args: unknown[]) => deleteRefreshCookie(...args),
}));

// --- Test data -------------------------------------------------------------

const authResponse = { accessTk: "tk", expiresIn: 900 };

const user: User = {
  name: "max",
  email: "max@mail.com",
  phone: "+43 555 555555",
  role: "USER",
};

// What the RTK Query hooks return: a result object, not the data itself.
// Only the fields AuthProvider reads are set.

const tokenLoaded = {
  data: authResponse,
  error: undefined,
  isError: false,
  isLoading: false,
  isFetching: false,
  refetch: vi.fn(),
};

const tokenRejected401 = {
  data: undefined,
  error: { status: 401 },
  isError: true,
  isLoading: false,
  isFetching: false,
  refetch: vi.fn(),
};

// 503 is what auth-actions returns when the backend is not reachable.
const tokenBackendDown = {
  data: undefined,
  error: { status: 503 },
  isError: true,
  isLoading: false,
  isFetching: false,
  refetch: vi.fn(),
};

const meLoaded = {
  data: user,
  error: undefined,
  isError: false,
  isLoading: false,
};

const meRejected401 = {
  data: undefined,
  error: { status: 401 },
  isError: true,
  isLoading: false,
};

const meRejected404 = {
  data: undefined,
  error: { status: 404 },
  isError: true,
  isLoading: false,
};

// Without a token the me query is skipped: no data, no error, not loading.
const meSkipped = {
  data: undefined,
  error: undefined,
  isError: false,
  isLoading: false,
};

// --- Setup -----------------------------------------------------------------

beforeEach(() => {
  vi.resetAllMocks();
  // jsdom's location is read-only, so the whole object is swapped instead
  // (put back after each test via `unstubGlobals` in vitest.config.ts).
  vi.stubGlobal("location", { ...window.location, replace });
});

// --- Tests -----------------------------------------------------------------

describe("AuthProvider", () => {
  it("renders the children when the token and the user are loaded", () => {
    // Arrange
    useGetAccessTkQuery.mockReturnValue(tokenLoaded);
    useGetMeQuery.mockReturnValue(meLoaded);

    // Act
    render(
      <AuthProvider>
        <p>Dashboard</p>
      </AuthProvider>,
    );

    // Assert
    expect(screen.getByText("Dashboard")).toBeInTheDocument();
    expect(deleteRefreshCookie).not.toHaveBeenCalled();
    expect(replace).not.toHaveBeenCalled();
  });

  it.each([
    {
      reason: "the user is rejected (401)",
      token: tokenLoaded,
      me: meRejected401,
    },
    {
      reason: "the user no longer exists (404)",
      token: tokenLoaded,
      me: meRejected404,
    },
    {
      reason: "the refresh token is rejected (401)",
      token: tokenRejected401,
      me: meSkipped,
    },
  ])(
    "deletes the cookie and redirects to sign-in when $reason",
    async ({ token, me }) => {
      // Arrange
      useGetAccessTkQuery.mockReturnValue(token);
      useGetMeQuery.mockReturnValue(me);

      // Act
      render(
        <AuthProvider>
          <p>Dashboard</p>
        </AuthProvider>,
      );

      // Assert
      expect(
        screen.getByText("Your session has expired …"),
      ).toBeInTheDocument();
      expect(screen.queryByText("Dashboard")).not.toBeInTheDocument();
      expect(deleteRefreshCookie).toHaveBeenCalled();
      // The redirect comes after `await deleteRefreshCookie()`, so wait for it.
      await waitFor(() =>
        expect(replace).toHaveBeenCalledWith("/sign-in?session=expired"),
      );
    },
  );

  it("keeps the cookie and shows the retry message when the backend is down", () => {
    // Arrange
    useGetAccessTkQuery.mockReturnValue(tokenBackendDown);
    useGetMeQuery.mockReturnValue(meSkipped);

    // Act
    render(
      <AuthProvider>
        <p>Dashboard</p>
      </AuthProvider>,
    );

    // Assert
    expect(screen.queryByText("Dashboard")).not.toBeInTheDocument();
    expect(
      screen.getByText("Der Server ist gerade nicht erreichbar."),
    ).toBeInTheDocument();
    expect(deleteRefreshCookie).not.toHaveBeenCalled();
    expect(replace).not.toHaveBeenCalledWith("/sign-in?session=expired");
  });

  it("refetches the token when retry is clicked", () => {
    // Arrange: the backend is down, so the retry button is shown
    useGetAccessTkQuery.mockReturnValue(tokenBackendDown);
    useGetMeQuery.mockReturnValue(meSkipped);
    render(
      <AuthProvider>
        <p>Dashboard</p>
      </AuthProvider>,
    );

    // Act
    fireEvent.click(screen.getByRole("button"));

    // Assert
    expect(tokenBackendDown.refetch).toHaveBeenCalled();
  });
});
