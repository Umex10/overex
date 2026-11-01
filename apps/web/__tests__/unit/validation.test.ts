import { describe, expect, it } from "vitest";
import { signInSchema, signUpSchema } from "@/features/auth/auth-validation";

// --- Test data -------------------------------------------------------------

const validSignUpForm = {
  name: "Max Mustermann",
  email: "max@example.com",
  phone: "+43660123456",
  password: "long-enough",
  terms: true,
};

// --- Helpers ---------------------------------------------------------------

/** The field names that failed, e.g. ["password"]. */
function failedFields(result: { success: boolean; error?: { issues: { path: PropertyKey[] }[] } }) {
  return result.success ? [] : result.error!.issues.map((issue) => issue.path[0]);
}

// --- Tests -----------------------------------------------------------------

describe("signUpSchema", () => {
  it("accepts a complete, valid form", () => {
    expect(signUpSchema.safeParse(validSignUpForm).success).toBe(true);
  });

  it("trims pasted spaces around the email instead of rejecting it", () => {
    const result = signUpSchema.parse({ ...validSignUpForm, email: "  max@example.com " });
    expect(result.email).toBe("max@example.com");
  });

  it("requires at least 8 password characters, like the backend", () => {
    const result = signUpSchema.safeParse({ ...validSignUpForm, password: "1234567" });
    expect(failedFields(result)).toEqual(["password"]);
  });

  it("requires at least 2 name characters", () => {
    const result = signUpSchema.safeParse({ ...validSignUpForm, name: "A" });
    expect(failedFields(result)).toEqual(["name"]);
  });

  it("trims before counting: spaces do not make a name long enough", () => {
    const result = signUpSchema.safeParse({ ...validSignUpForm, name: "  A  " });
    expect(failedFields(result)).toEqual(["name"]);
  });

  it("requires at least 10 phone characters", () => {
    const result = signUpSchema.safeParse({ ...validSignUpForm, phone: "066012345" });
    expect(failedFields(result)).toEqual(["phone"]);
  });

  it("requires the terms to be accepted", () => {
    const result = signUpSchema.safeParse({ ...validSignUpForm, terms: false });
    expect(failedFields(result)).toEqual(["terms"]);
  });

  it("reports every invalid field at once", () => {
    const result = signUpSchema.safeParse({ name: "", email: "nope", phone: "1", password: "", terms: false });
    expect(failedFields(result).sort()).toEqual(["email", "name", "password", "phone", "terms"]);
  });

  it("has no role field: a smuggled role is dropped from the parsed values", () => {
    const result = signUpSchema.parse({ ...validSignUpForm, role: "ADMIN" });
    expect(result).not.toHaveProperty("role");
  });
});

describe("signInSchema", () => {
  it("accepts any non-empty password (no length hint on sign-in)", () => {
    expect(signInSchema.safeParse({ email: "max@example.com", password: "x" }).success).toBe(true);
  });

  it("rejects an empty password and an invalid email", () => {
    const result = signInSchema.safeParse({ email: "nope", password: "" });
    expect(failedFields(result).sort()).toEqual(["email", "password"]);
  });
});
