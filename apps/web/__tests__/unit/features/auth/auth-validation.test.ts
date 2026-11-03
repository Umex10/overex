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
function failedFields(result: {
  success: boolean;
  error?: { issues: { path: PropertyKey[] }[] };
}) {
  return result.success
    ? []
    : result.error!.issues.map((issue) => issue.path[0]);
}

// --- Tests -----------------------------------------------------------------

describe("signUpSchema", () => {
  it("accepts a complete, valid form", () => {
    // Act
    const result = signUpSchema.safeParse(validSignUpForm);

    // Assert
    expect(result.success).toBe(true);
  });

  it("trims pasted spaces around the email instead of rejecting it", () => {
    // Act
    const result = signUpSchema.parse({
      ...validSignUpForm,
      email: "  max@example.com ",
    });

    // Assert
    expect(result.email).toBe("max@example.com");
  });

  // `requires` is only the test name, the check runs on `field`.
  it.each([
    {
      requires: "at least 8 password characters, like the backend",
      change: { password: "1234567" },
      field: "password",
    },
    {
      requires: "at least 2 name characters",
      change: { name: "A" },
      field: "name",
    },
    {
      requires: "at least 2 name characters after trimming",
      change: { name: "  A  " },
      field: "name",
    },
    {
      requires: "at least 10 phone characters",
      change: { phone: "066012345" },
      field: "phone",
    },
    {
      requires: "the terms to be accepted",
      change: { terms: false },
      field: "terms",
    },
  ])("requires $requires", ({ change, field }) => {
    // Act
    const result = signUpSchema.safeParse({ ...validSignUpForm, ...change });

    // Assert
    expect(failedFields(result)).toEqual([field]);
  });

  it("reports every invalid field at once", () => {
    // Act
    const result = signUpSchema.safeParse({
      name: "",
      email: "nope",
      phone: "1",
      password: "",
      terms: false,
    });

    // Assert
    expect(failedFields(result).sort()).toEqual([
      "email",
      "name",
      "password",
      "phone",
      "terms",
    ]);
  });

  it("has no role field: a smuggled role is dropped from the parsed values", () => {
    // Act
    const result = signUpSchema.parse({ ...validSignUpForm, role: "ADMIN" });

    // Assert
    expect(result).not.toHaveProperty("role");
  });
});

describe("signInSchema", () => {
  it("accepts any non-empty password (no length hint on sign-in)", () => {
    // Act
    const result = signInSchema.safeParse({
      email: "max@example.com",
      password: "x",
    });

    // Assert
    expect(result.success).toBe(true);
  });

  it("rejects an empty password and an invalid email", () => {
    // Act
    const result = signInSchema.safeParse({ email: "nope", password: "" });

    // Assert
    expect(failedFields(result).sort()).toEqual(["email", "password"]);
  });
});
