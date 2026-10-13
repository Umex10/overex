# Architecture & flow diagrams

This document explains how Overex's pieces fit together and — most importantly —
**which service calls which, and in what order**. All diagrams are Mermaid, so
GitHub renders them automatically.

---

## 1. The components

```mermaid
flowchart TB
    subgraph Browser
        UI["Next.js pages<br/>sign-in · sign-up · dashboard"]
        RTK["Redux Toolkit Query cache<br/>(holds the access token)"]
    end

    subgraph "Next.js server (BFF)"
        PROXY["proxy.ts<br/>route protection"]
        ACTIONS["Server Actions<br/>auth-action.ts"]
    end

    subgraph "Spring Boot — Overex API (/api/v1)"
        SEC["SecurityConfig<br/>OAuth2 resource server (JwtDecoder)"]
        AUTH["AuthController /auth/**"]
        ME["MeController /me, /admin/**"]
        JWKS["JwksController<br/>/.well-known/jwks.json"]
        JWT["JwtService<br/>signs with the private key"]
        KEYS["KeyConfig<br/>RSA key pair from PEM files"]
        DB[("PostgreSQL<br/>users")]
    end

    OTHER["Other microservices<br/>verify tokens themselves"]

    UI --> RTK
    UI -->|"navigations"| PROXY
    UI -->|"mutations/queries"| ACTIONS
    RTK -->|"Bearer access token"| ME
    ACTIONS -->|"HTTP + refresh cookie"| AUTH
    AUTH --> JWT
    AUTH --> DB
    SEC --> ME
    ME --> DB
    KEYS -->|"private key"| JWT
    KEYS -->|"public key"| SEC
    KEYS -->|"public key"| JWKS
    OTHER -->|"GET jwks.json (once, cached)"| JWKS
```

**Why a BFF?** The auth calls go through **Next.js Server Actions**, not directly
from the browser. That lets the server read/write the `refresh_tk` cookie as
**HTTP-only** — it is never exposed to client-side JavaScript. The short-lived
access token, by contrast, lives only in the RTK Query cache in memory and is
attached as a `Bearer` header to normal API calls.

---

## 2. Sign-up

```mermaid
sequenceDiagram
    autonumber
    participant U as Browser (form)
    participant A as Next.js Server Action
    participant B as Spring Boot API
    participant DB as PostgreSQL

    U->>A: signUp(values) via RTK mutation
    A->>B: POST /api/v1/auth/sign-up { name, email, phone, password }
    Note over B: email trimmed + lower-cased,<br/>role is always USER
    B->>DB: unique email & phone check
    alt email or phone taken
        B-->>A: 400 ApiError { errors:[{field,message}] }
        A-->>U: errors replayed onto the form fields
    else ok
        B->>DB: save user (password hashed)
        B-->>A: 201 { accessTk, expiresIn } + Set-Cookie: refresh_tk (HttpOnly)
        A-->>U: store cookie (server) + cache access token (RTK)
        U->>U: redirect to /dashboard
    end
```

---

## 3. Sign-in

```mermaid
sequenceDiagram
    autonumber
    participant U as Browser (form)
    participant A as Next.js Server Action
    participant B as Spring Boot API

    U->>A: signIn(email, password) via RTK mutation
    A->>B: POST /api/v1/auth/sign-in
    alt wrong credentials
        B-->>A: 401 ApiError { message: "The credentials are incorrect." }
        A-->>U: error toast (no per-field hint — no account enumeration)
    else ok
        B-->>A: 200 { accessTk, expiresIn } + Set-Cookie: refresh_tk
        A-->>U: cache access token, redirect to /dashboard
    end
```

---

## 4. Loading the protected dashboard (silent refresh + /me)

```mermaid
sequenceDiagram
    autonumber
    participant U as Browser
    participant P as proxy.ts (middleware)
    participant AP as AuthProvider
    participant A as Server Action
    participant B as Spring Boot API

    U->>P: GET /dashboard
    alt no refresh_tk cookie
        P-->>U: 307 redirect to /sign-in
    else cookie present
        P-->>U: allow
        U->>AP: render (shell) layout
        AP->>A: getAccessTk()
        A->>B: GET /api/v1/auth/access-tk (Cookie: refresh_tk)
        B-->>A: 200 { accessTk }
        A-->>AP: access token cached in RTK
        AP->>B: GET /api/v1/me (Authorization: Bearer accessTk)
        B-->>AP: 200 { name, email, phone, role }
        AP-->>U: render dashboard 🎉
    end
```

### When the session cannot be restored

`proxy.ts` only checks that the cookie **exists**, not that it is valid. If the
backend rejects it (expired, signed with a key that was replaced, or the account
was deleted), `AuthProvider` deletes the cookie and sends the user to
`/sign-in?session=expired`, which shows "Your session has expired". Without that
step the user would sit on an empty dashboard, and `/sign-in` would keep sending
them back because the cookie is still there.

### Access token expiry while the page stays open

The access token lives 15 minutes. When an API call comes back `401`, the shared
RTK Query base query fetches a new access token with the refresh cookie and
repeats the call once (parallel calls share one renewal). If the renewal fails
too, the case above applies.

### The reverse: already signed in

The same cookie check works the other way around. If you **already** have a
`refresh_tk` cookie and open the landing page (`/`), `/sign-in` or `/sign-up`,
`proxy.ts` sends you **straight to `/dashboard`** — no reason to show the
marketing or auth pages again. The landing-page redirect adds a `?redirected=1`
flag, and the dashboard shows a short "you're already signed in" note so the
convenience redirect is visible rather than magic. (The Overex logo in the
header intentionally links to `/`, so clicking it from the dashboard demonstrates
this exact bounce.)

---

## 5. How a request gets authenticated on the backend

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant F as BearerTokenAuthenticationFilter
    participant D as JwtDecoder
    participant CV as JwtAuthenticationConverter
    participant H as Controller

    C->>F: request with Authorization: Bearer <accessTk>
    F->>D: decode(accessTk)
    Note over D: RS256 signature (public key),<br/>exp / nbf, type_tk == ACCESS
    alt token invalid, expired or a refresh token
        D-->>C: 401 + WWW-Authenticate: Bearer error="invalid_token"
    else valid
        D-->>F: Jwt (claims)
        F->>CV: convert(jwt)
        CV-->>F: authorities from "roles" claim (ROLE_USER / ROLE_ADMIN)
        F->>H: SecurityContext set → controller (@AuthenticationPrincipal Jwt)
        H-->>C: response (or 403 if @PreAuthorize role check fails)
    end
```

All of this is Spring Security's built-in **OAuth2 resource server**, configured
in `SecurityConfig`; there is no custom filter. The token alone is enough: no
database lookup happens before the controller. `MeController` reads the user id
from the token's `sub` claim. See [SECURITY.md](SECURITY.md) for the details.

> Want the *fully detailed* call graph — every hop from controller through
> service to repository and database, the bean dependency graph and the filter
> chain order? See **[BACKEND_FLOW.md](BACKEND_FLOW.md)**.

---

## 6. Token & cookie model

| Token | Lifetime | Where it lives | Sent how |
|-------|----------|----------------|----------|
| **Access token** | 15 minutes | RTK Query cache (memory) | `Authorization: Bearer …` header |
| **Refresh token** | 30 days | `refresh_tk` cookie: HTTP-only, SameSite=Lax, Secure in production | browser cookie jar → Next.js Server Actions (BFF) → backend |

The backend is **stateless** — it stores no sessions. Signing out simply deletes
the refresh cookie; any still-valid access token expires on its own within 15
minutes.

---

## 7. Verifying Overex tokens in another service (JWKS)

Other services never receive the private key and never ask Overex whether a
token is valid. They fetch the **public** key once and verify every token
locally.

```mermaid
sequenceDiagram
    autonumber
    participant U as Client
    participant A as Overex
    participant S as Other service

    S->>A: GET /api/v1/.well-known/jwks.json (on first use, then cached)
    A-->>S: { keys: [ { kty: RSA, alg: RS256, use: sig, n, e } ] }
    U->>A: POST /api/v1/auth/sign-in
    A-->>U: accessTk (signed with the private key)
    U->>S: request with Authorization: Bearer <accessTk>
    S->>S: verify signature with the cached public key,<br/>exp, and type_tk == ACCESS
    S-->>U: response (or 401)
```

In a Spring service this is one property, `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`,
pointing at the JWKS URL, plus the `type_tk == ACCESS` validator (see
[SECURITY.md](SECURITY.md)). The integration test
`BackendApplicationTests#shouldVerifyAccessTkWithPublishedJwks` plays exactly
this role.
