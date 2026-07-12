import { describe, expect, it } from "vitest";
import { getErrorMessage } from "./messages";
import { ApiError } from "@/lib/api/client";

describe("getErrorMessage", () => {
  it("returns the mapped message for a known error code", () => {
    const err = new ApiError(400, "raw message", "AUTHENTICATION_FAILED");
    expect(getErrorMessage(err, "fallback")).toBe(
      "メールアドレスまたはパスワードが正しくありません",
    );
  });

  it("returns the fallback for an unknown error code", () => {
    const err = new ApiError(400, "raw message", "SOME_UNKNOWN_CODE");
    expect(getErrorMessage(err, "fallback")).toBe("fallback");
  });

  it("returns the fallback for non-ApiError values", () => {
    expect(getErrorMessage(new Error("boom"), "fallback")).toBe("fallback");
    expect(getErrorMessage("boom", "fallback")).toBe("fallback");
  });
});
