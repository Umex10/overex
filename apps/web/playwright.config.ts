import { defineConfig, devices } from "@playwright/test";

/**
 * Playwright config for Overex's end-to-end tests.
 *
 * Two projects run in sequence:
 *   1. `setup` registers the bootstrap account and saves its storage state
 *      (the refresh cookie).
 *   2. `features` reuses that storage state, so its specs start signed in.
 *      Specs that need a signed-out browser reset the state themselves.
 *
 * Everything runs on its own ports, next to a normal dev setup:
 *   - test database on 5433 (docker-compose.test.yml, started by `test:e2e`),
 *   - backend on 8081 (Spring profile `e2e`, fresh schema on every start),
 *   - frontend on 3001 as a production build (`next build` + `next start`):
 *     closer to production than `next dev`, and pages are interactive right
 *     away instead of being compiled on the first visit.
 */
export default defineConfig({
  testDir: "__tests__/e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  // A test that only passes on retry hides a bug; retries stay for CI only.
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "html",
  use: {
    baseURL: "http://localhost:3001",
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "setup",
      testMatch: /auth\.setup\.ts/,
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "features",
      testIgnore: [/auth\.setup\.ts/],
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/chromium.json",
      },
      dependencies: ["setup"],
    },
  ],
  // Playwright starts BOTH servers and waits until each answers before running
  // any test. Never reused: a leftover backend would keep the accounts of the
  // previous run, and the bootstrap sign-up would fail as "already taken".
  webServer: [
    {
      command: "npm run e2e:backend",
      // /v3/api-docs is public (permitAll) and returns 200 once Spring is up.
      url: "http://localhost:8081/api/v1/v3/api-docs",
      reuseExistingServer: false,
      timeout: 180_000,
    },
    {
      command: "npm run e2e:web",
      url: "http://localhost:3001",
      reuseExistingServer: false,
      timeout: 180_000,
    },
  ],
});
