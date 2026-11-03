# Backend flow — in detail (controller → service → repository → DB)

This document traces **every call** the Spring Boot backend makes, per endpoint,
all the way from the controller down to the repository and the database, including
the security filter chain and the error branches. If [ARCHITECTURE.md](ARCHITECTURE.md)
is the bird's-eye view, this is the wiring diagram.

All diagrams are Mermaid (rendered automatically by GitHub).

---

## 0. Bean / dependency graph

Who is injected into whom (constructor injection via Lombok `@RequiredArgsConstructor`),
grouped by layer.

```mermaid
flowchart TD
    subgraph web["Web layer (controllers)"]
        AC["AuthController<br/>/auth/**"]
        MC["MeController<br/>/me, /admin/**"]
        JC["JwksController<br/>/.well-known/jwks.json"]
        GEH["GlobalExceptionHandler<br/>@RestControllerAdvice"]
    end

    subgraph service["Service layer"]
        AS["AuthService"]
        JS["JwtService<br/>signs + reads tokens"]
    end

    subgraph keys["Keys"]
        KC["KeyConfig<br/>(@Bean factory)"]
        RKF["RsaKeyFiles<br/>PEM read / write"]
        PRIV["RSAPrivateKey"]
        PUB["RSAPublicKey"]
    end

    subgraph security["Security layer"]
        SC["SecurityConfig<br/>(@Bean factory)"]
        JD["JwtDecoder<br/>(Nimbus, RS256)"]
        JAC["JwtAuthenticationConverter<br/>roles → ROLE_*"]
        CUDS["CustomUserDetailsService<br/>implements UserDetailsService"]
        CUD["CustomUserDetails<br/>implements UserDetails"]
        AM["AuthenticationManager<br/>(Spring bean)"]
        PE["PasswordEncoder<br/>(delegating / bcrypt)"]
    end

    subgraph persistence["Persistence layer"]
        UR["UserRepository<br/>extends JpaRepository"]
        DB[("PostgreSQL<br/>users table")]
    end

    AC --> AS
    AC --> JS
    MC --> UR
    JC --> PUB

    AS --> UR
    AS --> PE
    AS --> AM

    JS --> PRIV
    JS --> PUB
    JS --> UR

    KC -. "uses" .-> RKF
    KC -. "builds" .-> PRIV
    KC -. "builds" .-> PUB

    JD --> PUB
    CUDS --> UR
    CUDS -. "creates" .-> CUD
    AM -. "delegates to" .-> CUDS
    AM -. "verifies with" .-> PE

    SC -. "builds" .-> JD
    SC -. "builds" .-> JAC
    SC -. "exposes" .-> AM
    SC -. "builds" .-> PE

    UR --> DB

    AS -. "throws OwnException" .-> GEH
    AM -. "throws AuthenticationException" .-> GEH
    MC -. "throws AccessDeniedException" .-> GEH
```

> `UserDetailsService` is an interface; the concrete bean is
> `CustomUserDetailsService`. It is only used for the **password check on
> sign-in** (through the `AuthenticationManager`). Requests with a bearer token
> never touch it: the `JwtDecoder` verifies the token on its own.
>
> On startup, before any bean exists, `DevKeyGenerator` writes a dev key pair
> through `RsaKeyFiles` when no key files are there yet (local runs only; see
> [RUNNING.md](RUNNING.md#jwt-keys)).

---

## 1. The security filter chain (runs before every controller)

Order of the filters in the one `SecurityFilterChain` built by `SecurityConfig`.
There is no custom filter: `.oauth2ResourceServer(...)` adds Spring's
`BearerTokenAuthenticationFilter`.

```mermaid
flowchart LR
    R["request"] --> SCHF["SecurityContextHolderFilter"]
    SCHF --> CORS["CorsFilter"]
    CORS --> BTAF["BearerTokenAuthenticationFilter ⭐<br/>JwtDecoder + JwtAuthenticationConverter"]
    BTAF --> AAF["AnonymousAuthenticationFilter"]
    AAF --> AZF["AuthorizationFilter<br/>(checks the route rules)"]
    AZF --> DS["DispatcherServlet → controller"]
```

Route rules (from `SecurityConfig.securityFilterChain`):

| Matcher | Rule |
|---------|------|
| `OPTIONS /**` | permit all (CORS pre-flight) |
| `/auth/**` | permit all (public) |
| `/.well-known/jwks.json` | permit all (public key for other services) |
| `/swagger-ui/**`, `/v3/api-docs/**` | permit all |
| everything else | `authenticated()` |

> A request **without** a token simply passes `BearerTokenAuthenticationFilter`
> and is rejected by the `AuthorizationFilter` on protected routes (401). A
> request with an **invalid** token is rejected by the filter itself (401 with
> `WWW-Authenticate: Bearer error="invalid_token"`).

---

## 2. `POST /auth/sign-up`

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant AC as AuthController
    participant AS as AuthService
    participant UR as UserRepository
    participant PE as PasswordEncoder
    participant JS as JwtService
    participant DB as PostgreSQL
    participant GEH as GlobalExceptionHandler

    C->>AC: POST /auth/sign-up (@Valid SignUpAccountRequestDto)
    Note over AC: @Valid runs Bean Validation first;<br/>failure → MethodArgumentNotValidException → GEH → 400
    AC->>AS: signUp(dto)
    AS->>UR: findByEmail(email)
    UR->>DB: SELECT … WHERE email = ?
    DB-->>UR: Optional<User>
    UR-->>AS: Optional<User>
    AS->>UR: findByPhone(phone)
    UR->>DB: SELECT … WHERE phone = ?
    DB-->>AS: Optional<User>

    alt email or phone already taken
        AS->>GEH: throw OwnException(fieldErrors)
        GEH-->>C: 400 ApiError { errors:[{field,message}] }
    else unique
        AS->>PE: encode(rawPassword)
        PE-->>AS: "{bcrypt}…" hash
        AS->>UR: save(User{…, email=lower-cased, role=USER, password=hash})
        UR->>DB: INSERT INTO users …
        DB-->>AS: persisted User (with generated UUID)
        AS-->>AC: User
        AC->>JS: createAccessTk(user)
        JS-->>AC: access JWT (15 min, RS256)
        AC->>JS: createRefreshTk(user)
        JS-->>AC: refresh JWT (30 days, RS256)
        Note over AC: adds Cookie refresh_tk (HttpOnly, Path=/, 30d)
        AC-->>C: 201 { accessTk, expiresIn:900 } + Set-Cookie: refresh_tk
    end
```

---

## 3. `POST /auth/sign-in`

Note the two steps: the `AuthenticationManager` verifies the credentials (it
loads the user through `CustomUserDetailsService` and checks the password), and
then `AuthService` loads the `User` entity again, because the tokens need its id
and role.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant AC as AuthController
    participant AS as AuthService
    participant AM as AuthenticationManager
    participant CUDS as CustomUserDetailsService
    participant UR as UserRepository
    participant PE as PasswordEncoder
    participant JS as JwtService
    participant DB as PostgreSQL
    participant GEH as GlobalExceptionHandler

    C->>AC: POST /auth/sign-in (@Valid SignInAccountRequestDto)
    AC->>AS: authenticate(email, password)
    AS->>AM: authenticate(UsernamePasswordAuthenticationToken)
    AM->>CUDS: loadUserByUsername(email)
    CUDS->>UR: findByEmail(email)
    UR->>DB: SELECT … WHERE email = ?
    DB-->>CUDS: User
    CUDS-->>AM: CustomUserDetails
    AM->>PE: matches(rawPassword, storedHash)

    alt bad credentials / unknown user
        AM->>GEH: throw AuthenticationException
        GEH-->>C: 401 ApiError { message:"The credentials are incorrect." }
    else ok
        AM-->>AS: Authentication (authenticated)
        AS->>UR: findByEmail(email)  ⟵ second load
        UR->>DB: SELECT …
        DB-->>AS: User
        AS-->>AC: User
        AC->>JS: createAccessTk(user)
        AC->>JS: createRefreshTk(user)
        AC-->>C: 200 { accessTk, expiresIn:900 } + Set-Cookie: refresh_tk
    end
```

> **Detail worth knowing:** the user is loaded **twice** on sign-in — once
> inside the `AuthenticationManager` and once in `AuthService`. It's correct,
> just a minor optimization opportunity.

---

## 4. `GET /auth/access-tk` (silent refresh)

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant AC as AuthController
    participant JS as JwtService
    participant UR as UserRepository
    participant DB as PostgreSQL

    C->>AC: GET /auth/access-tk (Cookie: refresh_tk)
    AC->>JS: extractRefreshTk(request)
    Note over JS: reads the refresh_tk cookie value
    alt cookie missing/empty
        JS-->>AC: null
        AC-->>C: 401 "The refresh token is missing"
    else present
        JS-->>AC: refreshTk
        AC->>JS: validateRefreshTk(refreshTk)
        Note over JS: verify RS256 signature with the public key,<br/>check exp and type_tk == REFRESH
        JS->>UR: findById(subject)
        UR->>DB: SELECT … WHERE id = ?
        DB-->>JS: User
        JS-->>AC: User
        AC->>JS: createAccessTk(user)
        AC-->>C: 200 { accessTk, expiresIn:900 }  (refresh cookie NOT rotated)
    end
```

> Any invalid refresh cookie → **401** "The credentials are incorrect.": an
> access token sent as refresh cookie fails the `type_tk` check, and a broken
> signature, an expired or a malformed token is turned into a
> `BadCredentialsException` inside `JwtService` (handled like a wrong password).

---

## 5. `GET /me` (protected route — the full chain)

The resource server authenticates the request from the token alone; the
controller then loads the user by the id in the token's `sub` claim.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant BTAF as BearerTokenAuthenticationFilter
    participant JD as JwtDecoder
    participant JAC as JwtAuthenticationConverter
    participant SCH as SecurityContextHolder
    participant MC as MeController
    participant UR as UserRepository
    participant DB as PostgreSQL
    participant GEH as GlobalExceptionHandler

    C->>BTAF: GET /me (Authorization: Bearer <accessTk>)
    BTAF->>JD: decode(accessTk)
    Note over JD: RS256 signature (public key),<br/>exp / nbf, type_tk == ACCESS
    alt token invalid / expired / refresh token
        JD-->>C: 401 WWW-Authenticate: Bearer error="invalid_token"
    else valid
        JD-->>BTAF: Jwt
        BTAF->>JAC: convert(jwt) → roles claim → ROLE_*
        BTAF->>SCH: setAuthentication(JwtAuthenticationToken)
        BTAF->>MC: continue chain → me(@AuthenticationPrincipal Jwt)
        MC->>UR: findById(UUID.fromString(jwt.getSubject()))
        UR->>DB: SELECT … WHERE id = ?
        alt user deleted meanwhile
            MC->>GEH: throw EntityNotFoundException
            GEH-->>C: 404 ErrorResponse
        else found
            DB-->>MC: User
            MC->>MC: UserResponseDto.from(user)  (drops the password)
            MC-->>C: 200 { name, email, phone, role }
        end
    end
```

---

## 6. `GET /admin/ping` (role-based authorization)

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant BTAF as BearerTokenAuthenticationFilter
    participant MSI as "@PreAuthorize interceptor"
    participant MC as MeController
    participant GEH as GlobalExceptionHandler

    C->>BTAF: GET /admin/ping (Bearer accessTk)
    Note over BTAF: authenticates as in §5,<br/>authorities from the roles claim = ROLE_USER or ROLE_ADMIN
    BTAF->>MSI: invoke MeController.adminPing()
    Note over MSI: @PreAuthorize("hasRole('ADMIN')")
    alt role is ADMIN
        MSI->>MC: proceed
        MC-->>C: 200 "pong — you are an ADMIN 👑"
    else role is USER
        MSI->>GEH: throw AccessDeniedException
        GEH-->>C: 403 ApiError { message:"You do not have permission…" }
    end
```

---

## 7. Where each thing lives

| Concern | Class | Path |
|---------|-------|------|
| Public auth endpoints | `AuthController` | `auth/AuthController.java` |
| Sign-up rules + credential check | `AuthService` | `auth/AuthService.java` |
| JWT create (RS256) + refresh-token check | `JwtService` | `auth/JwtService.java` |
| Per-request auth (JwtDecoder, roles converter) | `SecurityConfig` | `auth/security/config/SecurityConfig.java` |
| Key pair as beans | `KeyConfig` | `auth/config/KeyConfig.java` |
| PEM key files: read, write, generate | `RsaKeyFiles` | `auth/config/RsaKeyFiles.java` |
| Dev key pair on first start | `DevKeyGenerator` | `auth/config/DevKeyGenerator.java` |
| Public key for other services | `JwksController` | `auth/JwksController.java` |
| Principal lookup | `CustomUserDetailsService` | `auth/security/CustomUserDetailsService.java` |
| `User` → `UserDetails` adapter | `CustomUserDetails` | `auth/security/CustomUserDetails.java` |
| Protected example routes | `MeController` | `user/MeController.java` |
| Entity | `User` | `user/User.java` |
| Repository | `UserRepository` | `user/UserRepository.java` |
| Error → HTTP mapping | `GlobalExceptionHandler` | `common/exception/GlobalExceptionHandler.java` |
| Swagger / Authorize button | `OpenApiConfig` | `common/config/OpenApiConfig.java` |
