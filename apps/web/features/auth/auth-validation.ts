import * as z from "zod";

/**
 * Validation rules for the auth forms, shared by the pages (react-hook-form)
 * and the server actions (types). Kept outside the pages so they can be unit
 * tested and copied into another frontend together with the forms.
 *
 * The constraints mirror the backend's Bean Validation, so the user sees the
 * same rule before the request is even sent. Inputs are trimmed: a stray space
 * from copy & paste should not make an email "invalid".
 */

/** Sign-up: name >= 2, valid email, phone >= 10, password >= 8, terms accepted. */
export const signUpSchema = z.object({
  name: z.string().trim().min(2, "Name must be at least 2 characters."),
  email: z.string().trim().email("Please enter a valid email address."),
  phone: z
    .string()
    .trim()
    .min(10, "Please enter a valid phone number (at least 10 characters)."),
  password: z.string().min(8, "Password must be at least 8 characters."),
  // UI-only: never sent to the API.
  terms: z.boolean().refine((val) => val === true, {
    message: "You must accept the terms.",
  }),
});

export type SignUpFormValues = z.infer<typeof signUpSchema>;

/**
 * Sign-in: only checks that both fields are filled and the email looks like
 * one. No length rule on the password: the backend answers wrong credentials
 * with one generic message, and a length hint would reveal more than that.
 */
export const signInSchema = z.object({
  email: z.string().trim().email("Please enter a valid email address."),
  password: z.string().min(1, "Password is required."),
});

export type SignInFormValues = z.infer<typeof signInSchema>;
