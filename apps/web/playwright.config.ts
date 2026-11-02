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
  /*
   * `workers` = how many tests run at the same time at most (one browser
   * process each). `fullyParallel` = what is handed out to the workers: whole
   * files (false) or single tests (true).
   *
   * Example: file A has the tests A1–A3, file B has B1–B2, each takes 1 s.
   *
   *   workers: 1                       W1: A1 → A2 → A3 → B1 → B2      5 s
   *
   *   workers: 2, fullyParallel: false W1: A1 → A2 → A3                3 s
   *     (one file per worker)          W2: B1 → B2 → idle, may not take A3
   *
   *   workers: 2, fullyParallel: true  W1: A1 → A3 → B2                3 s
   *     (any free worker takes the     W2: A2 → B1
   *      next test, from any file)
   *
   * When to use what:
   * - fullyParallel: true (here) when every test is independent: it creates
   *   its own data in Arrange (newAccount()) and runs in its own browser
   *   context. Fastest, and the order never matters. This is the goal.
   * - Tests that build on each other (e.g. one creates an object, the next
   *   reads it) are a smell: if the first one fails, all others fail too.
   *   If it cannot be avoided, mark only that file with
   *   `test.describe.configure({ mode: "serial" })` (in order, one worker,
   *   the rest is skipped after a failure) and keep fullyParallel for the rest.
   * - workers: 1 when parallel runs would get in each other's way (one
   *   shared backend and database) or the machine is small: on CI here.
   */
  fullyParallel: true,
  // `test.only` would silently skip every other test: on CI it fails the run.
  // `!!` turns the string/undefined of process.env.CI into a real boolean.
  forbidOnly: !!process.env.CI,
  // A test that only passes on retry hides a bug; retries stay for CI only.
  retries: process.env.CI ? 1 : 0,
  // undefined = Playwright's default: half of the CPU cores.
  workers: process.env.CI ? 1 : undefined,
  // Console list plus an HTML report that is never opened on its own (also not
  // after a failure). Open it by hand with `npx playwright show-report`.
  reporter: [["list"], ["html", { open: "never" }]],
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
