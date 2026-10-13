# Testing

Overex has test layers across the backend and the web app. All of them pass
out of the box.

| Layer | Tool | Location | What it proves |
|-------|------|----------|----------------|
| Backend unit, slice + integration | JUnit 5, Mockito, MockMvc, Spring Security Test | `apps/backend/src/test` | Token logic, key loading, sign-up rules, the auth endpoints, token verification, role-based access, the JWKS endpoint |
| Web unit | Vitest + Testing Library | `apps/web/__tests__/unit` | Form rules, cookie parsing, route gate (`proxy.ts`), access-token renewal, components |
| Web end-to-end | Playwright (Chromium) | `apps/web/__tests__/e2e` | The real browser flow against the real backend + database |

---

## Backend tests

```bash
cd apps/backend
./mvnw test
```

Uses an **in-memory H2 database** in PostgreSQL-compatibility mode (the `test`
profile, `src/test/resources/application-test.properties`) — no Docker required,
runs in seconds.

No key files are needed either: the tests create their own RSA key
pair at runtime (`utils/TestKeys`).

`./mvnw verify` additionally writes a JaCoCo coverage report to
`apps/backend/target/site/jacoco/index.html` (the CI uploads it as the
`backend-reports` artifact).

The tests are split into three layers:

**Unit** (`unit/`, no Spring context, dependencies mocked):
- `JwtServiceTest`: access/refresh token claims, refresh token validation (wrong key, expired, malformed, wrong type, user gone), cookie extraction
- `AuthServiceTest`: sign-up with free/taken email and phone, sign-in with valid/invalid credentials
- `CustomUserDetailsServiceTest`: email lookup for the password check
- `RsaKeyFilesTest`: reading the PEM key files (valid, broken, unreadable) and a write/read round trip
- `DevKeyGeneratorTest`: the dev key pair is created only when both files are missing and the switch is on

**Slice** (`slice/`, `@WebMvcTest`: only the web layer, services mocked):
- `AuthControllerTest`: the three `/auth` endpoints, error envelopes, bean validation
- `MeControllerTest`: `/me` and `/admin/ping` through the real `SecurityConfig`, with real signed tokens and `@PreAuthorize`
- `JwksControllerTest`: the public JWKS endpoint, without any private key material

**Integration** (`integration/BackendApplicationTests`, the whole app, nothing mocked):
the real `KeyConfig` loads keys written to a temp folder; the Spring context
starts once and is shared by all tests, the database is emptied before each.
Covers sign-up → sign-in → `/me`, the refresh cookie (valid → new access
token, invalid → 401), admin vs. user role,
duplicate sign-up, the published JWKS, and a decoder built only from
`jwks.json` (as another microservice would) that accepts access tokens and
rejects refresh tokens.

---

## Web unit tests

```bash
cd apps/web
npm run test:unit          # one-shot
npm run test:unit:watch    # watch mode
```

Fast tests without servers or a browser:
- `validation.test.ts`: the sign-up/sign-in rules (8-character password, trimmed email, terms, no role field)
- `refresh-cookie.test.ts`: reading the refresh token from the backend's `Set-Cookie` headers
- `proxy.test.ts`: which route redirects where, with and without the refresh cookie
- `reauth.test.ts`: an expired access token is renewed once and the request repeated; a rejected renewal gives up
- `utils`, `AuthTips`, `Celebration`: small helpers and components

Type and lint checks: `npm run typecheck`, `npm run lint`.

Every run also writes `test-reports/junit.xml` (configured in
`vitest.config.ts`); the CI uploads it as an artifact.

---

## End-to-end tests

The e2e suite runs the **whole stack**: a throwaway PostgreSQL, the backend on
the `e2e` profile (port 8081), and a **production build** of the Next.js
frontend (port 3001). One command orchestrates everything:

```bash
cd apps/web
npm run test:e2e
```

What that command does:
1. `docker compose -f docker-compose.test.yml up -d` — starts the throwaway DB on port **5433**.
2. Playwright starts **both** servers (backend via `./mvnw`, frontend via
   `next build` + `next start` into its own `.next-e2e` folder) and waits until
   each answers. Servers are never reused, so every run starts with an empty
   database. The ports don't clash with a dev setup on 3000/8080.
3. Runs the specs: the `setup` project registers a bootstrap account and saves
   its cookie; `signed-in.spec.ts` starts with that cookie, `signed-out.spec.ts`
   without one.
4. Tears the database back down and exits with Playwright's result (non-zero if
   a test failed).

Locally there are no retries: a test that only passes on the second try is a
bug, not bad luck.

What the specs cover:

| Signed out | Signed in |
|------------|-----------|
| Protected page → `/sign-in` | Reload keeps the session (silent refresh) |
| Sign-up creates a `USER`; dashboard shows the data from `GET /me`; no role field | `/`, `/sign-in`, `/sign-up` forward to the dashboard |
| Invalid input is explained before anything is sent | Sign-out ends the session |
| Taken email/phone marked on the fields, cleared on edit | Refresh cookie is `HttpOnly` + `SameSite=Lax`, invisible to `document.cookie` |
| Wrong credentials: generic error, no field hint | |
| Sign-in ignores the email's capitalization | |
| A rejected refresh cookie is removed → `/sign-in?session=expired` | |

Run it with the Playwright UI instead:

```bash
npm run test:e2e:ui
```

### Why a separate `e2e` profile and database?

So tests never touch your development data. `application-e2e.properties` points
the backend at the disposable Postgres on `5433`, recreates the schema on every
boot (`ddl-auto=create-drop`), and runs on port `8081` so it can sit next to a
normally-running dev backend on `8080`.

---

## In CI

Each layer has its own GitHub Actions workflow (`backend-verify-ci`,
`web-unit-ci`, `web-e2e-ci`, plus `web-build-ci` for lint, types and the
build). Their reports are attached to every run as artifacts; see
[CI.md](CI.md).
