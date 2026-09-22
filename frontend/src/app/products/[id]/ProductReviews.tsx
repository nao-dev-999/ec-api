"use client";

import { useEffect, useState, useCallback } from "react";
import { Star } from "lucide-react";
import {
  getProductReviews,
  createReview,
  updateReview,
  deleteReview,
  type Review,
  type ReviewSummary,
} from "@/lib/api/reviews";
import { getMe } from "@/lib/api/me";
import { ApiError } from "@/lib/api/client";
import { getErrorMessage } from "@/lib/errors/messages";

function StarRatingInput({
  value,
  onChange,
}: {
  value: number;
  onChange: (rating: number) => void;
}) {
  return (
    <div style={{ display: "flex", gap: 2 }}>
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          onClick={() => onChange(n)}
          aria-label={`${n} 点`}
          style={{
            background: "transparent",
            padding: 2,
            color: n <= value ? "#f59e0b" : "var(--muted)",
          }}
        >
          <Star size={20} fill={n <= value ? "currentColor" : "none"} />
        </button>
      ))}
    </div>
  );
}

function StarDisplay({ rating }: { rating: number }) {
  return (
    <span
      aria-label={`評価 ${rating} / 5`}
      style={{ display: "inline-flex", gap: 1 }}
    >
      {[1, 2, 3, 4, 5].map((n) => (
        <Star
          key={n}
          size={14}
          fill={n <= rating ? "currentColor" : "none"}
          style={{ color: "#f59e0b" }}
        />
      ))}
    </span>
  );
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString("ja-JP");
}

const PAGE_SIZE = 10;

export default function ProductReviews({ productId }: { productId: number }) {
  const [reviews, setReviews] = useState<Review[] | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [summary, setSummary] = useState<ReviewSummary | null>(null);
  const [myCustomerId, setMyCustomerId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formMessage, setFormMessage] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);

  const load = useCallback(() => {
    getProductReviews(productId, page, PAGE_SIZE)
      .then((data) => {
        setReviews(data.reviews.content);
        setTotalPages(Math.max(1, data.reviews.totalPages));
        setSummary(data.summary);
      })
      .catch((err) =>
        setError(getErrorMessage(err, "レビューの取得に失敗しました")),
      );
  }, [productId, page]);

  useEffect(() => {
    load();
    getMe({ suppressAuthRedirect: true })
      .then((me) => setMyCustomerId(me.id ?? null))
      .catch(() => setMyCustomerId(null));
  }, [load]);

  const myReview = reviews?.find((r) => r.customerId === myCustomerId) ?? null;

  function startEdit(review: Review) {
    setEditingId(review.id);
    setRating(review.rating);
    setComment(review.comment ?? "");
    setFormMessage(null);
  }

  function cancelEdit() {
    setEditingId(null);
    setRating(0);
    setComment("");
    setFormMessage(null);
  }

  async function handleSubmit() {
    setFormMessage(null);
    if (rating < 1 || rating > 5) {
      setFormMessage("評価を選択してください");
      return;
    }
    setSubmitting(true);
    try {
      if (editingId !== null && myReview) {
        await updateReview(editingId, {
          rating,
          comment: comment || undefined,
          version: myReview.version,
        });
      } else {
        await createReview({
          productId,
          rating,
          comment: comment || undefined,
        });
      }
      cancelEdit();
      load();
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setFormMessage("レビューの投稿にはログインが必要です");
        return;
      }
      setFormMessage(getErrorMessage(err, "レビューの投稿に失敗しました"));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(review: Review) {
    if (!window.confirm("このレビューを削除しますか？")) return;
    try {
      await deleteReview(review.id);
      if (editingId === review.id) cancelEdit();
      load();
    } catch (err) {
      setFormMessage(getErrorMessage(err, "レビューの削除に失敗しました"));
    }
  }

  if (error) return <p style={{ color: "red" }}>{error}</p>;
  if (reviews === null || summary === null)
    return <p>レビューを読み込み中...</p>;

  return (
    <section style={{ marginTop: 32 }}>
      <h2>レビュー</h2>

      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 8,
          marginBottom: 16,
        }}
      >
        <StarDisplay rating={Math.round(summary.averageRating)} />
        <span>
          {summary.averageRating.toFixed(1)}({summary.reviewCount}件)
        </span>
      </div>

      {reviews.length === 0 && (
        <p style={{ color: "var(--muted)" }}>まだレビューがありません</p>
      )}

      <ul>
        {reviews.map((review) => (
          <li
            key={review.id}
            style={{ flexDirection: "column", alignItems: "stretch" }}
          >
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <div>
                <StarDisplay rating={review.rating} />
                <span
                  style={{
                    marginLeft: 8,
                    color: "var(--muted)",
                    fontSize: "0.85rem",
                  }}
                >
                  {review.customerName ?? "匿名"} ・{" "}
                  {formatDate(review.createdAt)}
                </span>
              </div>
              {review.customerId === myCustomerId && (
                <span style={{ display: "flex", gap: 8 }}>
                  <button onClick={() => startEdit(review)}>編集</button>
                  <button onClick={() => handleDelete(review)}>削除</button>
                </span>
              )}
            </div>
            {review.comment && <p style={{ marginTop: 4 }}>{review.comment}</p>}
          </li>
        ))}
      </ul>

      {totalPages > 1 && (
        <div
          style={{
            display: "flex",
            justifyContent: "center",
            gap: 12,
            marginTop: 8,
            marginBottom: 16,
          }}
        >
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => setPage((p) => p - 1)}
          >
            前へ
          </button>
          <span style={{ alignSelf: "center", color: "var(--muted)" }}>
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            次へ
          </button>
        </div>
      )}

      {!myReview || editingId !== null ? (
        <div className="form-card" style={{ marginTop: 24, maxWidth: "none" }}>
          <h2 style={{ marginBottom: 12 }}>
            {editingId !== null ? "レビューを編集" : "レビューを投稿"}
          </h2>
          <div>
            <StarRatingInput value={rating} onChange={setRating} />
          </div>
          <div>
            <textarea
              placeholder="商品の感想を入力してください(任意)"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              rows={3}
              maxLength={1000}
              style={{ width: "100%" }}
            />
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <button onClick={handleSubmit} disabled={submitting}>
              {submitting
                ? "送信中..."
                : editingId !== null
                  ? "更新する"
                  : "投稿する"}
            </button>
            {editingId !== null && (
              <button
                type="button"
                onClick={cancelEdit}
                style={{
                  background: "var(--border)",
                  color: "var(--foreground)",
                }}
              >
                キャンセル
              </button>
            )}
          </div>
          {formMessage && <p style={{ marginTop: 8 }}>{formMessage}</p>}
          <p
            style={{ marginTop: 8, color: "var(--muted)", fontSize: "0.8rem" }}
          >
            ※ 配送完了した購入実績がある商品のみ投稿できます
          </p>
        </div>
      ) : null}
    </section>
  );
}
