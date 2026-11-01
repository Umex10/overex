import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "path";

/**
 * Vitest config for fast, browser-free unit/component tests (jsdom).
 * Picks up the unit tests (__tests__/unit) and the integration tests
 * (__tests__/integration: several real units together, only the network
 * mocked). Both run in the same command; the Playwright e2e folder is
 * excluded so the suites never collide.
 */
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./__tests__/setup.ts"],
    // Puts back every global replaced with vi.stubGlobal (fetch, location …)
    // after each test, so no fake leaks into the next one.
    unstubGlobals: true,
    include: [
      "__tests__/unit/**/*.{test,spec}.{ts,tsx}",
      "__tests__/integration/**/*.{test,spec}.{ts,tsx}",
    ],
    exclude: ["__tests__/e2e/**", "node_modules/**"],
    // Console output plus a JUnit XML file with every test (name, duration,
    // error message of failed ones), locally and on CI. On GitHub Actions
    // failed tests are also marked on the run page. Listing reporters
    // replaces the default one, so "default" is named as well.
    reporters: [
      "default",
      "junit",
      ...(process.env.GITHUB_ACTIONS ? ["github-actions"] : []),
    ],
    outputFile: { junit: "test-reports/junit.xml" },
    // Coverage only for logic (plain .ts: functions with input and output).
    // React components (.tsx) are left to the Playwright e2e tests, so an
    // untested button never lowers the number. Run with `npm run test:coverage`.
    coverage: {
      provider: "v8",
      include: [
        "proxy.ts",
        "features/**/*.ts",
        "redux/**/*.ts",
        // Exception to the .ts rule: the session gate decides between logout
        // and retry, and the "backend down" case is hard to reach in e2e.
        "features/auth/components/auth-provider.tsx",
      ],
      exclude: [
        // Only types, no code that could run.
        "**/*-types.ts",
        // RTK Query endpoints: wiring only, covered by the e2e tests.
        "**/*-api.ts",
        // Configuration only.
        "redux/store.ts",
        "redux/hooks.ts",
      ],
      reporter: ["text", "html"],
      reportsDirectory: "coverage",
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "."),
    },
  },
});
