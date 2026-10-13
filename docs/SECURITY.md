# Security model & production hardening

## The model

- **Stateless JWT.** The backend stores no sessions. Every request is
  authenticated purely from its `Authorization: Bearer <accessTk>` header by
  Spring Security's **OAuth2 resource server**: the `JwtDecoder` checks the
  signature, the expiry and that the token is an access token (`type_tk = ACCESS`).
  No database lookup is needed per request.
- **Asymmetric signing (RS256).** Only Overex holds the **private key** and
  signs tokens with it. The matching **public key** is published at
  `/.well-known/jwks.json`, so any other service can verify Overex tokens
  itself, without calling back. A leaked public key is harmless; a leaked
  private key lets anyone forge tokens for any user.
- **Refresh tokens are no access tokens.** Both are signed with the same key, so
  a refresh token has a valid signature too. The `type_tk` claim tells them
  apart: the resource server only accepts `ACCESS`, the refresh endpoint only
  `REFRESH`. **Every other service that verifies Overex tokens must add the same
  `type_tk = ACCESS` check**, otherwise it would accept a 30-day refresh token
  as a bearer token.
- **Two tokens, two lifetimes.**
  - *Access token* (15 min): returned in the JSON body, kept in memory (the RTK
    Query cache), sent as a Bearer header. Short-lived to limit blast radius.
  - *Refresh token* (30 days): delivered as an **HTTP-only** cookie (`refresh_tk`)
    so client-side JavaScript can never read it — this mitigates token theft via
    XSS. `SameSite=Lax` keeps it out of cross-site POSTs (CSRF), and it is
    `Secure` (HTTPS only) in production builds.
- **The BFF pattern (web).** The browser never talks to the auth endpoints
  directly; Next.js Server Actions do, server-side, so the refresh cookie stays
  HTTP-only end to end.
- **Dead sessions end cleanly.** A refresh cookie the backend rejects is deleted
  and the user lands on `/sign-in?session=expired`; an access token that expires
  while the page is open is renewed once on a `401`.
- **Passwords** are hashed with Spring Security's delegating `PasswordEncoder`
  (bcrypt by default) — never stored or logged in clear text. Sign-up requires at
  least 8 characters. The forms use `method="post"` and keep the submit button
  disabled until the page is interactive, so a password can never end up in a
  URL.
- **No account enumeration on sign-in.** Wrong credentials return a generic
  `401 "The credentials are incorrect."` with no hint about which field was
  wrong, and sign-in has no password length rule that could hint either.
  *Trade-off:* sign-up does say "email already taken" (good usability, but it
  reveals that an account exists). Rate limiting (see below) keeps that from
  being scraped at scale.
- **Emails** are trimmed and lower-cased on sign-up and sign-in, so case
  differences neither create duplicate accounts nor lock anyone out.
- **No self-assigned roles.** Sign-up has no role field; every new account is a
  `USER`, and a `"role"` sent anyway is ignored. Admins are promoted
  deliberately, e.g. directly in the database.
- **Roles** travel in the access token's `roles` claim. The
  `JwtAuthenticationConverter` in `SecurityConfig` maps them to Spring
  authorities (`USER` → `ROLE_USER`), enforced with `@PreAuthorize` (see
  `MeController#adminPing`).

## Two subtle bugs this codebase already fixes

1. **`StackOverflowError` on every rejected token.** `SecurityConfig` exposes
   Spring's global `AuthenticationManager`, which is also the parent of the
   resource server's manager. Spring can only build it when there is exactly
   one `UserDetailsService` bean (`CustomUserDetailsService`). With none or
   several, it falls back to a proxy that delegates to itself, and every
   rejected bearer token ends in endless recursion. Keep exactly one
   `UserDetailsService`, and import it in every `@WebMvcTest` that imports
   `SecurityConfig`.
2. **`@PreAuthorize` → 500 instead of 403.** Method-security denials throw
   `AccessDeniedException` during the controller invocation, where a catch-all
   `@ExceptionHandler(Exception.class)` would turn it into a 500. An explicit
   `AccessDeniedException` handler maps it to a clean **403**.

## Before you ship to production

- [ ] **Use a production RSA key pair.** Generate it once with
      `scripts/generate-jwt-keys.sh <folder outside the repo>`, store both files as
      a secret and let your platform mount them into the container, then point
      `JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` at them (details in
      [RUNNING.md](RUNNING.md#jwt-keys)). The Docker image never generates keys, so
      missing ones stop the backend at startup. Never reuse a dev key, never commit
      `private.pem`, and give only the backend access to it — other services need
      just the public key (`/.well-known/jwks.json`).
- [ ] **Serve over HTTPS** and set the refresh cookie's `secure` flag to `true`
      — in `AuthController#createAuthResponseDto` (backend) and in
      `setRefreshCookie` (`apps/web/actions/auth-action.ts`), which already
      switches to `secure` in production builds.
- [ ] **Lock down CORS** via `FRONTEND_URL` to your real origin(s); drop the
      localhost defaults in `SecurityConfig` if you don't need them.
- [ ] **Switch the schema strategy.** `ddl-auto=update` is convenient for getting
      started; use a migration tool (Flyway/Liquibase) for production.
- [ ] Consider **refresh-token rotation / revocation** if your threat model needs
      it (the current refresh endpoint does not rotate the cookie).
- [ ] Add **rate limiting** on `/auth/**` to slow brute-force attempts.
