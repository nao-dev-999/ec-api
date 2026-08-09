"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Star, Trash2, ChevronLeft, ChevronRight } from "lucide-react";
import {
  getAdminReviews,
  deleteAdminReview,
  type AdminReview,
} from "@/lib/api/adminReviews";
import ConfirmModal from "../ConfirmModal";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

const PAGE_SIZE = 20;

function formatDate(value: string | undefined) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("ja-JP");
}

function Stars({ rating }: { rating: number }) {
  return (
    <span aria-label={`評価 ${rating} / 5`}>
      {Array.from({ length: 5 }, (_, i) => (
        <Star
          key={i}
          size={14}
          fill={i < rating ? "currentColor" : "none"}
          style={{ color: "#f59e0b", marginRight: 1 }}
        />
      ))}
    </span>
  );
}

export default function AdminReviewsPage() {
  const { showToast } = useToast();
  const [reviews, setReviews] = useState<AdminReview[] | null>(null);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminReview | null>(null);

  useEffect(() => {
    getAdminReviews(page - 1, PAGE_SIZE)
      .then((result) => {
        setReviews(result.content ?? []);
        setTotalPages(Math.max(1, result.totalPages ?? 1));
        setTotalElements(result.totalElements ?? 0);
      })
      .catch((err) =>
        setError(getErrorMessage(err, "レビュー一覧の取得に失敗しました")),
      );
  }, [page]);

  async function handleDelete() {
    if (!deleteTarget) return;
    try {
      await deleteAdminReview(deleteTarget.id);
      setReviews((prev) => prev!.filter((r) => r.id !== deleteTarget.id));
      setTotalElements((n) => Math.max(0, n - 1));
      showToast("レビューを削除しました");
    } catch (err) {
      showToast(getErrorMessage(err, "削除に失敗しました"), "error");
    } finally {
      setDeleteTarget(null);
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (reviews === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <div className="page-heading">
        <h1>
          <Star size={22} />
          レビュー管理
        </h1>
        <span className="page-count">全 {totalElements} 件</span>
      </div>

      <ul className="admin-list">
        {reviews.length === 0 && (
          <p style={{ padding: "16px 0", color: "var(--muted)" }}>
            レビューはまだありません
          </p>
        )}
        {reviews.map((review) => (
          <li key={review.id}>
            <div className="card-head">
              <div>
                <Link href={`/products/${review.productId}`}>
                  {review.productName ?? `商品 #${review.productId}`}
                </Link>
                <p className="card-desc" style={{ marginTop: 2 }}>
                  {review.customerName ?? `顧客 #${review.customerId}`}
                </p>
              </div>
              <span className="card-updated">
                {formatDate(review.createdAt)}
              </span>
            </div>

            <Stars rating={review.rating} />

            {review.comment && (
              <p className="card-desc" style={{ marginTop: 8 }}>
                {review.comment}
              </p>
            )}

            <div className="card-divider" />

            <div className="card-footer">
              <span />
              <span className="item-actions">
                <button
                  className="icon-btn"
                  title="削除"
                  onClick={() => setDeleteTarget(review)}
                >
                  <Trash2 size={16} />
                </button>
              </span>
            </div>
          </li>
        ))}
      </ul>

      <div className="pagination">
        <span className="page-count">
          {reviews.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1}〜
          {Math.min(page * PAGE_SIZE, totalElements)} 件 / 全{totalElements} 件
        </span>
        <div className="page-buttons">
          <button
            className="page-btn"
            disabled={page <= 1}
            onClick={() => setPage((p) => p - 1)}
          >
            <ChevronLeft size={16} />
          </button>
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
            <button
              key={n}
              className={"page-btn" + (n === page ? " active" : "")}
              onClick={() => setPage(n)}
            >
              {n}
            </button>
          ))}
          <button
            className="page-btn"
            disabled={page >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      <ConfirmModal
        open={deleteTarget !== null}
        title="レビューを削除"
        message={`このレビューを削除しますか？この操作は取り消せません。`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </main>
  );
}
