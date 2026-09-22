import { describe, expect, it, vi, beforeEach } from "vitest";
import { apiFetch } from "./client";
import {
  getProductReviews,
  createReview,
  updateReview,
  deleteReview,
} from "./reviews";

vi.mock("./client", () => ({
  apiFetch: vi.fn(),
}));

describe("reviews api", () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset();
  });

  it("getProductReviews requests the given page/size", async () => {
    vi.mocked(apiFetch).mockResolvedValue({
      reviews: { content: [], page: 1, size: 5, totalElements: 0, totalPages: 1 },
      summary: { averageRating: 0, reviewCount: 0 },
    });

    await getProductReviews(10, 1, 5);

    expect(apiFetch).toHaveBeenCalledWith(
      "/api/customer/products/10/reviews?page=1&size=5",
    );
  });

  it("getProductReviews defaults to page 0 / size 20", async () => {
    vi.mocked(apiFetch).mockResolvedValue({
      reviews: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 1 },
      summary: { averageRating: 0, reviewCount: 0 },
    });

    await getProductReviews(10);

    expect(apiFetch).toHaveBeenCalledWith(
      "/api/customer/products/10/reviews?page=0&size=20",
    );
  });

  it("createReview posts to /api/customer/reviews", async () => {
    vi.mocked(apiFetch).mockResolvedValue({});

    await createReview({ productId: 10, rating: 5, comment: "最高" });

    expect(apiFetch).toHaveBeenCalledWith("/api/customer/reviews", {
      method: "POST",
      body: JSON.stringify({ productId: 10, rating: 5, comment: "最高" }),
    });
  });

  it("updateReview puts to /api/customer/reviews/{id}", async () => {
    vi.mocked(apiFetch).mockResolvedValue({});

    await updateReview(1, { rating: 4, version: 0 });

    expect(apiFetch).toHaveBeenCalledWith("/api/customer/reviews/1", {
      method: "PUT",
      body: JSON.stringify({ rating: 4, version: 0 }),
    });
  });

  it("deleteReview deletes /api/customer/reviews/{id}", async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined);

    await deleteReview(1);

    expect(apiFetch).toHaveBeenCalledWith("/api/customer/reviews/1", {
      method: "DELETE",
    });
  });
});
