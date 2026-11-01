import useBreadcrumbs from "@/features/shell/hooks/use-breadcrumbs";
import { makeStore, type AppStore } from "@/redux/store";
import { renderHook } from "@testing-library/react";
import { createElement } from "react";
import { Provider } from "react-redux";
import { beforeEach, describe, expect, it, vi } from "vitest";

// --- Mocks -----------------------------------------------------------------

// usePathname is replaced, so each test decides which page is open.
const pathname = vi.fn();
vi.mock("next/navigation", () => ({
  usePathname: () => pathname(),
}));

// --- Setup -----------------------------------------------------------------

let store: AppStore;

beforeEach(() => {
  store = makeStore();
  pathname.mockReset();
});

// --- Helpers ---------------------------------------------------------------

/** Runs the hook inside the test store; `rerender` simulates a navigation. */
function renderUseBreadcrumbs() {
  const { rerender } = renderHook(() => useBreadcrumbs(), {
    wrapper: ({ children }) => createElement(Provider, { store, children }),
  });

  return rerender;
}

function storedBreadcrumbs() {
  return store.getState().routeContext.breadcrumbs;
}

// --- Tests -----------------------------------------------------------------

describe("useBreadcrumbs", () => {
  it("splits the current path into its segments", () => {
    pathname.mockReturnValue("/events/5");

    renderUseBreadcrumbs();

    expect(storedBreadcrumbs()).toEqual(["events", "5"]);
  });

  it("updates the segments when the path changes without remounting", () => {
    // Not "/events" at the start: that is the store's initial value and
    // would pass even if the hook never wrote anything.
    pathname.mockReturnValue("/events/5");
    const rerender = renderUseBreadcrumbs();

    pathname.mockReturnValue("/my-tickets");
    rerender();

    expect(storedBreadcrumbs()).toEqual(["my-tickets"]);
  });

  it("leaves no empty segments for the root path", () => {
    pathname.mockReturnValue("/");

    renderUseBreadcrumbs();

    expect(storedBreadcrumbs()).toEqual([]);
  });
});
