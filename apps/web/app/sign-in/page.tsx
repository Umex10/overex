"use client";

import Link from "next/link";
import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Spinner } from "@/components/ui/loader";
import { ThemeToggle } from "@/components/theme-toggle";
import { useHydrated } from "@/hooks/use-hydrated";
import { AuthTips } from "@/features/auth/components/auth-tips";
import { useSignInMutation } from "@/features/auth/auth-api";
import { signInSchema, type SignInFormValues } from "@/features/auth/auth-validation";
import type { ApiErrorBody } from "@/types/api-error";

/**
 * Sign-in page at `/sign-in`. Wires a Zod-validated react-hook-form to the
 * `useSignInMutation` RTK endpoint, shows toasts and redirects to the dashboard
 * on success.
 */
export default function SignInPage() {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);
  const hydrated = useHydrated();
  const [signIn, { isLoading: isSigningIn }] = useSignInMutation();

  const form = useForm<SignInFormValues>({
    resolver: zodResolver(signInSchema),
    defaultValues: { email: "", password: "" },
  });

  const onSubmit = async (values: SignInFormValues) => {
    const toastId = toast.loading("Signing in …", {
      className: "toast-loading",
    });
    try {
      await signIn(values).unwrap();
      toast.success("Signed in successfully!", {
        id: toastId,
        className: "toast-success",
      });
      router.push("/events");
    } catch (error) {
      const message = (error as ApiErrorBody)?.message || "Sign-in failed.";
      toast.error(message, { id: toastId, className: "toast-error" });
    }
  };

  return (
    <div className="flex min-h-screen">
      {/* Form column */}
      <main className="relative flex w-full flex-col justify-center px-6 py-12 sm:px-12 lg:w-1/2 lg:px-20">
        <Link
          href="/"
          className="absolute top-6 left-6 text-sm font-medium text-muted-foreground transition-opacity hover:opacity-60"
        >
          ← Back
        </Link>
        <div className="absolute top-6 right-6">
          <ThemeToggle />
        </div>

        <div className="mx-auto w-full max-w-sm">
          <div className="mb-8">
            <h1 className="text-3xl font-extrabold tracking-tight">
              Welcome back
            </h1>
            <p className="mt-2 text-sm text-muted-foreground">
              Sign in to get to your dashboard.
            </p>
          </div>

          <Suspense fallback={null}>
            <SessionExpiredNote />
          </Suspense>

          {/* method="post": should the browser ever submit the form itself, the
              password goes into the request body, never into the URL.
              noValidate: the Zod messages below the fields replace the
              browser's own bubbles (which would also block the submit event). */}
          <form
            method="post"
            noValidate
            className="flex flex-col gap-5"
            onSubmit={form.handleSubmit(onSubmit)}
          >
            <FieldGroup>
              <Controller
                name="email"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor="email">Email</FieldLabel>
                    <Input
                      {...field}
                      id="email"
                      data-testid="sign-in-email-input"
                      type="email"
                      placeholder="name@example.com"
                      autoComplete="email"
                      aria-invalid={fieldState.invalid}
                      className="h-auto rounded-lg px-3.5 py-2.5 text-sm"
                    />
                    {fieldState.invalid && (
                      <FieldError
                        errors={[fieldState.error]}
                        fieldName="sign-in-email"
                      />
                    )}
                  </Field>
                )}
              />

              <Controller
                name="password"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor="password">Password</FieldLabel>
                    <div className="flex items-stretch gap-2">
                      <Input
                        {...field}
                        id="password"
                        data-testid="sign-in-password-input"
                        type={showPassword ? "text" : "password"}
                        placeholder="Your password"
                        autoComplete="current-password"
                        aria-invalid={fieldState.invalid}
                        className="h-auto rounded-lg px-3.5 py-2.5 text-sm"
                      />
                      <Button
                        type="button"
                        variant="outline"
                        onClick={() => setShowPassword((p) => !p)}
                        className="h-auto rounded-lg px-3 text-xs"
                      >
                        {showPassword ? "Hide" : "Show"}
                      </Button>
                    </div>
                    {fieldState.invalid && (
                      <FieldError
                        errors={[fieldState.error]}
                        fieldName="sign-in-password"
                      />
                    )}
                  </Field>
                )}
              />
            </FieldGroup>

            <Button
              type="submit"
              data-testid="sign-in-submit-button"
              disabled={!hydrated || isSigningIn}
              className="mt-1 h-auto w-full rounded-xl py-3 text-sm font-semibold"
            >
              {isSigningIn ? "Signing in …" : "Sign in"}
              {isSigningIn && <Spinner className="ml-2 size-4 text-current" />}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-muted-foreground">
            No account yet?{" "}
            <Link
              href="/sign-up"
              className="font-semibold text-primary hover:opacity-70"
            >
              Sign up for free
            </Link>
          </p>
        </div>
      </main>

      {/* Tips panel (desktop only) */}
      <AuthTips variant="sign-in" />
    </div>
  );
}

/**
 * Shown when the dashboard sent the user here because their session could not
 * be renewed (refresh cookie expired, signed with an old key, or the account is
 * gone). The stale cookie has already been removed, so signing in again works.
 */
function SessionExpiredNote() {
  const expired = useSearchParams().get("session") === "expired";
  if (!expired) return null;

  return (
    <div
      role="status"
      data-testid="sign-in-session-expired-note"
      className="mb-6 rounded-xl border border-primary/30 bg-primary/10 px-4 py-3 text-sm text-foreground"
    >
      Your session has expired. Please sign in again.
    </div>
  );
}
