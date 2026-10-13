# Customizing Overex for your project

Overex is meant to be a starting point. Here are the changes you'll most likely
make.

---

## Change the roles

The roles are deliberately generic (`USER`, `ADMIN`). To use your own:

1. **Backend** — edit the enum in
   `apps/backend/src/main/java/com/overex/backend/common/entities/Role.java`:
   ```java
   public enum Role { OWNER, DRIVER, ACCOUNTANT }
   ```
   That's the single source of truth — it flows automatically into the JWT
   authorities (`ROLE_OWNER`, …) and every `@PreAuthorize("hasRole('OWNER')")`.
   Sign-up always assigns `Role.USER` (`AuthService#signUp`); point it at your
   default role.
2. **Web frontend** — update the union in `apps/web/types/User.ts` (it is only
   used to display the role; nobody picks a role in the form).

Users never choose their own role. Promote someone deliberately, e.g. with
`UPDATE users SET role = 'ADMIN' WHERE email = '…';`, or build an admin-only
endpoint for it.

---

## Add a protected endpoint

Backend — create a controller and read the authenticated user's id from the
verified token (`sub` claim):

```java
@RestController
@SecurityRequirement(name = "bearerAuth") // shows the lock in Swagger
public class ProjectController {
    @GetMapping("/projects")
    public List<Project> mine(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ...
    }

    @PostMapping("/admin/thing")
    @PreAuthorize("hasRole('ADMIN')")
    public void adminOnly() { ... }
}
```

Frontend — add an RTK Query endpoint next to `apps/web/redux/api/apis/me.ts`:

```ts
const projectsApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getProjects: builder.query<Project[], void>({ query: () => `/projects` }),
  }),
});
export const { useGetProjectsQuery } = projectsApi;
```

The access token is attached automatically. Put new pages under
`apps/web/app/(shell)/` and they're protected by `proxy.ts` + `AuthProvider` for free.

If the access token expired meanwhile, the request is renewed and repeated
automatically (see `redux/api/apiSlice.ts`).

---

## Take the auth into another Next.js app

Everything the web client needs for sign-up, sign-in and protected pages lives
in a handful of files under `apps/web`:

| Files | Role |
|-------|------|
| `app/sign-up/page.tsx`, `app/sign-in/page.tsx` | The forms (react-hook-form + Zod, field errors from the backend, session-expired note) |
| `lib/validation.ts` | The form rules, mirrored from the backend |
| `actions/auth-action.ts`, `lib/refresh-cookie.ts` | Server Actions that call the backend and keep the refresh token in an HTTP-only cookie |
| `redux/` (store, `apiSlice.ts`, `apis/auth.ts`, `apis/me.ts`) | RTK Query: access-token cache, automatic renewal on `401` |
| `components/AuthProvider.tsx`, `components/SignOutButton.tsx`, `app/(shell)/layout.tsx` | The gate for protected pages and sign-out |
| `proxy.ts` | Redirects between public and protected routes |
| `lib/config.ts`, `lib/use-hydrated.ts`, `types/` | Backend URL, small helpers, shared types |
| `components/ui/` | The shadcn/ui pieces the forms use (button, input, field, checkbox, …) |

The matching tests (`__tests__/unit`, `__tests__/e2e`) can come along as well.

---

## Re-theme

Colors and radius are CSS variables in `apps/web/app/globals.css` (a standard
shadcn/ui token set). Change `--primary` (and its dark-mode value) to rebrand.
The font is set in `apps/web/app/layout.tsx` (`next/font`).

---

## Remove the fun gag 🎉

The celebratory confetti on the dashboard is 100% optional and fully isolated.
To remove it completely:

1. Delete the folder `apps/web/components/fun/`.
2. In `apps/web/app/(shell)/dashboard/page.tsx`, remove the `import { Celebration }`
   line and the `<Celebration />` usage (both are marked with a `FUN GAG` comment).
3. *(optional)* Delete the `FUN GAG` block at the bottom of `apps/web/app/globals.css`.
4. *(optional)* Delete the test `apps/web/__tests__/unit/Celebration.test.tsx`.

Nothing else depends on it — the dashboard becomes a plain welcome screen.

The "tips" panel next to the auth forms (`apps/web/components/AuthTips.tsx`) is
separate from the gag; it's genuinely useful onboarding text. Replace it with
your own marketing, or remove the `<AuthTips />` usage in the sign-in/sign-up
pages if you prefer a single-column layout.

---

## Rename the project / package

- Backend Java package `com.overex.backend` → your package (rename folders +
  the `package`/`import` statements; `groupId` in `pom.xml`).
- Database name, container names and the JWT key locations
  (`JWT_PRIVATE_KEY_LOCATION`, `JWT_PUBLIC_KEY_LOCATION`) live in
  `apps/backend/src/main/resources/application*.properties` and
  `docker-compose*.yml`.
- Frontend package name in `apps/web/package.json`.
