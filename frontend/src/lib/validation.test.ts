import { describe, expect, it } from "vitest";
import { parsePrice, parseQuantity, parseStock } from "./validation";

describe("parsePrice", () => {
  it("accepts positive numbers", () => {
    expect(parsePrice("100")).toBe(100);
  });

  it("rejects zero, negative, and non-numeric input", () => {
    expect(parsePrice("0")).toBeNull();
    expect(parsePrice("-1")).toBeNull();
    expect(parsePrice("")).toBeNull();
    expect(parsePrice("abc")).toBeNull();
  });
});

describe("parseStock", () => {
  it("accepts zero and positive numbers", () => {
    expect(parseStock("0")).toBe(0);
    expect(parseStock("5")).toBe(5);
  });

  it("rejects negative and non-numeric input", () => {
    expect(parseStock("-1")).toBeNull();
    expect(parseStock("abc")).toBeNull();
  });
});

describe("parseQuantity", () => {
  it("accepts integers of 1 or more", () => {
    expect(parseQuantity(1)).toBe(1);
    expect(parseQuantity(5)).toBe(5);
  });

  it("rejects NaN and values below 1", () => {
    expect(parseQuantity(NaN)).toBeNull();
    expect(parseQuantity(0)).toBeNull();
    expect(parseQuantity(-1)).toBeNull();
  });
});
