# Swagger / OpenAPI

Overex ships a **live, interactive API explorer** powered by springdoc-openapi.
Because the auth routes and a `bearerAuth` security scheme are declared in code,
the UI gets a green **`Authorize 🔒`** button and shows a lock icon on every
protected route.

## Where it is

- **Built into the backend (use this one):**
  http://localhost:8080/api/v1/swagger-ui.html
  This is always in sync with the running code and has the Authorize button.
- **Raw OpenAPI document:**
  http://localhost:8080/api/v1/v3/api-docs
- **Standalone mirror container** (started by `docker compose up`):
  http://localhost:8082 — convenient if you want the docs on a fixed URL; it
  loads the live spec above.

## Testing a protected route in 4 steps

1. **Create an account** — expand `POST /auth/sign-up`, *Try it out*, send a body like:
   ```json
   { "name": "Ada", "email": "ada@example.com", "phone": "+431234567890", "password": "secret123" }
   ```
   (If the email is already taken you'll get a 400 — just sign in instead.)
2. **Sign in** — `POST /auth/sign-in` with the same email/password. Copy the
   `accessTk` value from the response body.
3. **Authorize** — click **`Authorize 🔒`** at the top, paste **only the token**
   (no `Bearer ` prefix — Swagger adds it), and confirm.
4. **Call a protected route** — try `GET /me`. It now sends the
   `Authorization: Bearer …` header automatically and returns your account.
   Try `GET /admin/ping` to see role-based authorization in action (403 unless
   your user's role is `ADMIN`).

## How it's wired

- `config/OpenApiConfig.java` declares the API info and the `bearerAuth`
  `@SecurityScheme` — that single annotation is what makes the Authorize button
  appear.
- Controllers opt a route into the lock icon with
  `@SecurityRequirement(name = "bearerAuth")` (see `MeController`). The public
  `/auth/**` routes deliberately leave it off.
