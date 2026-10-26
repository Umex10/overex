import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "path";

/**
 * Vitest config for fast, browser-free unit/component tests (jsdom).
 * Only picks up files under __tests__/unit; the Playwright e2e folder is
 * excluded so the two suites never collide.
 */
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./__tests__/unit/setup.ts"],
    include: ["__tests__/unit/**/*.{test,spec}.{ts,tsx}"],
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
