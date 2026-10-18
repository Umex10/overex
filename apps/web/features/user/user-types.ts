/**
 * The roles Overex ships with. Mirrors the backend `Role` enum.
 * Extend this (and the backend enum + the sign-up select) for your project.
 */
export type Role = "USER" | "ADMIN";

/**
 * The authenticated user as returned by `GET /me`.
 * Never contains the password.
 */
export interface User {
  name: string;
  email: string;
  phone: string;
  role: Role;
}
