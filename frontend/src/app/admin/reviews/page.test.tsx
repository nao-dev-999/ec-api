import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import AdminReviewsPage from "./page";
import { getAdminReviews, deleteAdminReview } from "@/lib/api/adminReviews";
import { ToastProvider } from "@/app/Toast";

vi.mock("@/lib/api/adminReviews", () => ({
  getAdminReviews: vi.fn(),
  deleteAdminReview: vi.fn(),
}));

function renderPage() {
  return render(
    <ToastProvider>
      <AdminReviewsPage />
    </ToastProvider>,
  );
}

function makeReview(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 1,
    productId: 10,
    productName: "テスト商品",
    customerId: 100,
    customerName: "山田",
    rating: 4,
    comment: "良かったです",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    version: 0,
    ...overrides,
  };
}

describe("AdminReviewsPage", () => {
  beforeEach(() => {
    vi.mocked(getAdminReviews).mockReset();
    vi.mocked(deleteAdminReview).mockReset();
  });

  it("renders the review list with the total count", async () => {
    vi.mocked(getAdminReviews).mockResolvedValue({
      content: [makeReview()],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });

    renderPage();

    expect(await screen.findByText("良かったです")).toBeInTheDocument();
    expect(screen.getByText("全 1 件")).toBeInTheDocument();
  });

  it("deletes a review after confirmation", async () => {
    vi.mocked(getAdminReviews).mockResolvedValue({
      content: [makeReview()],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    vi.mocked(deleteAdminReview).mockResolvedValue(undefined);

    renderPage();

    fireEvent.click(await screen.findByTitle("削除"));

    // ConfirmModal内の確定ボタン。一覧の削除アイコン(title="削除")も同じ
    // アクセシブルネームを持つため、モーダル表示後にDOM順で最後(=モーダル側)を選ぶ。
    const deleteButtons = await screen.findAllByRole("button", {
      name: "削除",
    });
    fireEvent.click(deleteButtons[deleteButtons.length - 1]);

    await waitFor(() => expect(deleteAdminReview).toHaveBeenCalledWith(1));
    expect(await screen.findByText("レビューはまだありません")).toBeInTheDocument();
  });
});
