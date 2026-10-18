import { describe, expect, it } from "vitest";
import { extractRefreshTk } from "@/features/auth/refresh-cookie";

describe("extractRefreshTk", () => {
  it("finds the token among several Set-Cookie headers", () => {
    expect(
      extractRefreshTk(["theme=dark; Path=/", "refresh_tk=abc.def.ghi; HttpOnly; Path=/; Max-Age=2592000"]),
    ).toBe("abc.def.ghi");
  });

  it("accepts a single header given as a string", () => {
    expect(extractRefreshTk("refresh_tk=abc.def.ghi; HttpOnly")).toBe("abc.def.ghi");
  });

  it("keeps '=' inside the value (Base64 padding)", () => {
    expect(extractRefreshTk("refresh_tk=abc==; Path=/")).toBe("abc==");
  });

  it("returns null without a refresh_tk cookie or with an empty one", () => {
    expect(extractRefreshTk(undefined)).toBeNull();
    expect(extractRefreshTk(["theme=dark"])).toBeNull();
    expect(extractRefreshTk("refresh_tk=; Max-Age=0")).toBeNull();
  });

  it("does not mistake a cookie that only starts the same way", () => {
    expect(extractRefreshTk("refresh_tk_old=abc")).toBeNull();
  });
});
