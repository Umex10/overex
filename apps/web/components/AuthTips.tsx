import { API_BASE_URL } from "@/lib/config";

/**
 * The right-hand "how this works" panel shown next to the auth forms.
 *
 * It's genuinely useful documentation for anyone who just cloned Overex: it
 * explains, in-place, what happens when you submit the form and where to look
 * next. Feel free to replace it with your own product marketing once you build
 * on top of Overex.
 */
export function AuthTips({
  variant,
}: {
  variant: "sign-in" | "sign-up";
}) {
  const tips =
    variant === "sign-up"
      ? [
          "Your details go to POST /auth/sign-up on the Spring backend.",
          "The password is hashed server-side — never stored in plain text.",
          "You instantly get an access token + an HTTP-only refresh cookie.",
          "Then you're redirected to the dashboard automatically.",
        ]
      : [
          "Your input goes to POST /auth/sign-in.",
          "On success the backend sets the refresh_tk cookie (HTTP-only).",
          "The short-lived access token lands in the RTK Query cache.",
          "proxy.ts protects every route except /, /sign-in and /sign-up.",
        ];

  return (
    <aside className="hidden w-1/2 flex-col justify-center gap-8 bg-primary px-16 text-primary-foreground lg:flex">
      <div className="flex flex-col gap-3">
        <div className="flex items-center gap-2">
          <span className="text-2xl">🔐</span>
          <span className="text-lg font-semibold tracking-tight">Overex</span>
        </div>
        <h2 className="text-2xl font-bold leading-tight">
          {variant === "sign-up"
            ? "An account, in seconds."
            : "Welcome back."}
        </h2>
        <p className="text-sm leading-relaxed opacity-80">
          A reusable auth system: Spring Boot + Next.js, JWT, roles, Swagger and
          tests. Clone it, start it, build.
        </p>
      </div>

      <ul className="flex flex-col gap-3">
        {tips.map((tip) => (
          <li key={tip} className="flex items-start gap-2.5 text-sm opacity-90">
            <span className="mt-0.5 shrink-0">✦</span>
            <span className="leading-snug">{tip}</span>
          </li>
        ))}
      </ul>

      <div className="rounded-lg bg-white/10 px-4 py-3 text-xs leading-relaxed opacity-90">
        <p className="font-semibold">Developer tip</p>
        <p className="mt-1">
          Explore the API routes live at{" "}
          <code className="rounded bg-white/15 px-1 py-0.5">
            {API_BASE_URL}/swagger-ui.html
          </code>{" "}
          — including the “Authorize 🔒” button.
        </p>
      </div>
    </aside>
  );
}
