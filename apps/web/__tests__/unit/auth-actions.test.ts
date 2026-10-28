import { signInAction, signUpAction } from "@/features/auth/auth-actions";
import {
  REFRESH_COOKIE,
  REFRESH_COOKIE_MAX_AGE,
} from "@/features/auth/refresh-cookie";
import { beforeEach, describe, expect, it, vi } from "vitest";

const post = vi.fn();
const get = vi.fn();

vi.mock("axios", async (importOriginal) => {
  const actual = await importOriginal<typeof import("axios")>();
  return {
    ...actual,
    default: {
      ...actual.default,
      create: () => ({
        post: (...args: unknown[]) => post(...args),
        get: (...args: unknown[]) => get(...args),
      }),
    },
  };
});

const cookieStore = {
  get: vi.fn(),
  set: vi.fn(),
  delete: vi.fn(),
};

vi.mock("next/headers", () => ({
  cookies: async () => cookieStore,
}));

beforeEach(() => {
  for (let mock of [
    post,
    get,
    cookieStore.get,
    cookieStore.set,
    cookieStore.delete,
  ]) {
    mock.mockReset();
  }
});

describe("auth-actions", () => {
  it("calls sign-up correctly", async () => {
    const input = { name: "", email: "", phone: "", password: "" };

    const refreshTkValue = "abc";

    post.mockResolvedValue({
      data: { accessTk: "tk", expiresIn: 900 },
      headers: { "set-cookie": [`refresh_tk=${refreshTkValue}; HttpOnly`] },
    });

    const res = await signUpAction({ ...input, terms: true });

    expect(post).toHaveBeenLastCalledWith("sign-up", input);

    expect(res).toEqual({
      success: true,
      data: { accessTk: "tk", expiresIn: 900 },
    });

    expect(cookieStore.set).toHaveBeenLastCalledWith({
      name: REFRESH_COOKIE,
      value: refreshTkValue,
      httpOnly: true,
      secure: false,
      path: "/",
      maxAge: REFRESH_COOKIE_MAX_AGE,
      sameSite: "lax",
    });
  });

  it("calls sign-in correctly", async () => {
    const input = { email: "", password: "" };

    const refreshTkValue = "abc";

    post.mockResolvedValue({
      data: { accessTk: "tk", expiresIn: 900 },
      headers: { "set-cookie": [`refresh_tk=${refreshTkValue}; HttpOnly`] },
    });

    const res = await signInAction(input);

    expect(post).toHaveBeenLastCalledWith("sign-in", input);

    expect(res).toEqual({
      success: true,
      data: { accessTk: "tk", expiresIn: 900 },
    });

    expect(cookieStore.set).toHaveBeenLastCalledWith({
      name: REFRESH_COOKIE,
      value: refreshTkValue,
      httpOnly: true,
      secure: false,
      path: "/",
      maxAge: REFRESH_COOKIE_MAX_AGE,
      sameSite: "lax",
    });
  });

  it("sign-in throws error", async () => {
    const input = { email: "", password: "" };

    post.mockRejectedValue({
      isAxiosError: true,
      response: { status: 401, data: "nope" },
    });

    const res = await signInAction(input);

    expect(post).toHaveBeenLastCalledWith("sign-in", input);

    expect(res).toEqual({
      success: false,
      error: { status: 401, message: "nope" },
    });

    expect(cookieStore.set).not.toHaveBeenCalled();
  });
});
