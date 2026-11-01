import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook } from "@testing-library/react";
import { Provider } from "react-redux";
import useBreadcrumbs from "@/features/shell/hooks/use-breadcrumbs";
import { makeStore, type AppStore } from "@/redux/store";

// --- Mocks -----------------------------------------------------------------

// usePathname is replaced, so each test decides which page is open.
const pathname = vi.fn();

vi.mock("next/navigation", () => ({
  usePathname: (...args: unknown[]) => pathname(...args),
}));

// --- Helpers ---------------------------------------------------------------

/** Runs the hook inside the test store; `rerender` simulates a navigation. */
function renderUseBreadcrumbs() {
  const { rerender } = renderHook(() => useBreadcrumbs(), {
    wrapper: ({ children }) => <Provider store={store}>{children}</Provider>,
  });

  return rerender;
}

function storedBreadcrumbs() {
  return store.getState().routeContext.breadcrumbs;
}

// --- Setup -----------------------------------------------------------------

let store: AppStore;

beforeEach(() => {
  vi.resetAllMocks();
  store = makeStore();
});

// --- Tests -----------------------------------------------------------------

describe("useBreadcrumbs", () => {
  it("splits the current path into its segments", () => {
    // Arrange
    pathname.mockReturnValue("/events/5");

    // Act
    renderUseBreadcrumbs();

    // Assert
    expect(storedBreadcrumbs()).toEqual(["events", "5"]);
  });

  it("updates the segments when the path changes without remounting", () => {
    // Arrange: not "/events" at the start, that is the store's initial value
    // and would pass even if the hook never wrote anything.
    pathname.mockReturnValue("/events/5");
    const rerender = renderUseBreadcrumbs();

    // Act
    pathname.mockReturnValue("/my-tickets");
    rerender();

    // Assert
    expect(storedBreadcrumbs()).toEqual(["my-tickets"]);
  });

  it("leaves no empty segments for the root path", () => {
    // Arrange
    pathname.mockReturnValue("/");

    // Act
    renderUseBreadcrumbs();

    // Assert
    expect(storedBreadcrumbs()).toEqual([]);
  });
});
