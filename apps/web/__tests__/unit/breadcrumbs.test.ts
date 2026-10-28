import useBreadcrumbs from "@/features/shell/hooks/use-breadcrumbs";
import { makeStore, type AppStore } from "@/redux/store";
import { renderHook } from "@testing-library/react";
import { createElement } from "react";
import { Provider } from "react-redux";
import { beforeEach, describe, expect, it, vi } from "vitest";

// usePathname is replaced, so each test decides which page is open.
const path = vi.fn();
vi.mock("next/navigation", () => ({
  usePathname: () => path(),
}));

let store: AppStore;

beforeEach(() => {
  store = makeStore();
  path.mockReset();
});

/** Runs the hook inside the test store; `rerender` simulates a navigation. */
function renderUseBreadcrumbs() {
  const { rerender } = renderHook(() => useBreadcrumbs(), {
    wrapper: ({ children }) => createElement(Provider, { store, children }),
  });

  return rerender;
}

function breadcrumbs() {
  return store.getState().routeContext.breadcrumbs;
}

describe("useBreadcrumbs", () => {
  it("splits the current path into its segments", () => {
    path.mockReturnValue("/events/5");

    renderUseBreadcrumbs();

    expect(breadcrumbs()).toEqual(["events", "5"]);
  });

  it("updates the segments when the path changes without remounting", () => {
    // Not "/events" at the start: that is the store's initial value and
    // would pass even if the hook never wrote anything.
    path.mockReturnValue("/events/5");
    const rerender = renderUseBreadcrumbs();

    path.mockReturnValue("/my-tickets");
    rerender();

    expect(breadcrumbs()).toEqual(["my-tickets"]);
  });

  it("leaves no empty segments for the root path", () => {
    path.mockReturnValue("/");

    renderUseBreadcrumbs();

    expect(breadcrumbs()).toEqual([]);
  });
});
