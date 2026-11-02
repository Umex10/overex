#!/usr/bin/env sh
# Runs the Playwright e2e tests with everything they need around them:
#   1. starts the throwaway test database (docker-compose.test.yml, port 5433),
#   2. runs Playwright, which starts backend + frontend itself (webServer),
#   3. always cleans up, even when tests fail, and exits with Playwright's result.
#
# Every argument is passed on to Playwright, e.g.
#   npm run test:e2e -- -g "clicks an event"
#   npm run test:e2e -- features/event/events.spec.ts

npm run e2e:db-up || exit 1

npx playwright test "$@"
status=$?

# The Spring process (npm -> mvnw -> java) can outlive Playwright and would
# block port 8081 for the next run.
fuser -k 8081/tcp 2>/dev/null
npm run e2e:db-down

exit $status
