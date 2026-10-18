import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { AuthTips } from "@/features/auth/components/auth-tips";

describe("AuthTips", () => {
  it("shows sign-up specific guidance", () => {
    render(<AuthTips variant="sign-up" />);
    expect(
      screen.getByText(/POST \/auth\/sign-up/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/An account, in seconds\./i)).toBeInTheDocument();
  });

  it("shows sign-in specific guidance", () => {
    render(<AuthTips variant="sign-in" />);
    expect(screen.getByText(/POST \/auth\/sign-in/i)).toBeInTheDocument();
    expect(screen.getByText(/Welcome back\./i)).toBeInTheDocument();
  });

  it("always points to the Swagger UI", () => {
    render(<AuthTips variant="sign-up" />);
    expect(screen.getByText(/swagger-ui\.html/i)).toBeInTheDocument();
  });
});
