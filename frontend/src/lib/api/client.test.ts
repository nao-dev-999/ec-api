import { afterEach, describe, expect, it, vi } from "vitest";
import { apiFetch, ApiError } from "./client";

describe("apiFetch", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("returns parsed JSON on success", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        text: async () => "",
        json: async () => ({ id: 1 }),
      }),
    );

    await expect(apiFetch<{ id: number }>("/api/example")).resolves.toEqual({
      id: 1,
    });
  });

  it("returns undefined for 204 responses", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        status: 204,
        text: async () => "",
      }),
    );

    await expect(
      apiFetch<void>("/api/example", { method: "DELETE" }),
    ).resolves.toBeUndefined();
  });

  it("throws ApiError with the parsed code/message/details on failure", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        statusText: "Bad Request",
        text: async () =>
          JSON.stringify({
            message: "在庫が不足しています",
            code: "INSUFFICIENT_STOCK",
            details: { productId: "1" },
          }),
      }),
    );

    await expect(apiFetch("/api/example")).rejects.toMatchObject({
      status: 400,
      message: "在庫が不足しています",
      code: "INSUFFICIENT_STOCK",
      details: { productId: "1" },
    });
  });

  it("falls back to statusText when the error body is not JSON", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        statusText: "Internal Server Error",
        text: async () => "not json",
      }),
    );

    const error = await apiFetch("/api/example").catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(500);
  });
});
