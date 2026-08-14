import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import ProductReviews from "./ProductReviews";
import { getProductReviews } from "@/lib/api/reviews";
import { getMe } from "@/lib/api/me";

vi.mock("@/lib/api/reviews", () => ({
  getProductReviews: vi.fn(),
  createReview: vi.fn(),
  updateReview: vi.fn(),
  deleteReview: vi.fn(),
}));

vi.mock("@/lib/api/me", () => ({
  getMe: vi.fn(),
}));

function makeReview(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 1,
    productId: 10,
    customerId: 100,
    customerName: "山田",
    rating: 5,
    comment: "最高でした",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    version: 0,
    ...overrides,
  };
}

describe("ProductReviews", () => {
  beforeEach(() => {
    vi.mocked(getMe).mockRejectedValue(new Error("not logged in"));
  });

  it("shows the review list and average rating", async () => {
    vi.mocked(getProductReviews).mockResolvedValue({
      reviews: {
        content: [makeReview()],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      },
      summary: { averageRating: 5, reviewCount: 1 },
    });

    render(<ProductReviews productId={10} />);

    expect(await screen.findByText("最高でした")).toBeInTheDocument();
    expect(screen.getByText("5.0(1件)")).toBeInTheDocument();
  });

  it("shows an empty state when there are no reviews", async () => {
    vi.mocked(getProductReviews).mockResolvedValue({
      reviews: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 1 },
      summary: { averageRating: 0, reviewCount: 0 },
    });

    render(<ProductReviews productId={10} />);

    expect(
      await screen.findByText("まだレビューがありません"),
    ).toBeInTheDocument();
  });

  it("requests the next page when 次へ is clicked", async () => {
    vi.mocked(getProductReviews).mockResolvedValue({
      reviews: {
        content: [makeReview()],
        page: 0,
        size: 10,
        totalElements: 20,
        totalPages: 2,
      },
      summary: { averageRating: 5, reviewCount: 20 },
    });

    render(<ProductReviews productId={10} />);

    const nextButton = await screen.findByRole("button", { name: "次へ" });
    fireEvent.click(nextButton);

    await waitFor(() =>
      expect(getProductReviews).toHaveBeenCalledWith(10, 1, 10),
    );
  });
});
