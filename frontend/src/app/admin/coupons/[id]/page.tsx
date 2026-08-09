"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import {
  getAdminCoupon,
  updateAdminCoupon,
  type AdminCoupon,
} from "@/lib/api/adminCoupons";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

function toDateInputValue(value: string | null) {
  if (!value) return "";
  return value.slice(0, 10);
}

export default function AdminCouponDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const couponId = Number(id);
  const { showToast } = useToast();

  const [coupon, setCoupon] = useState<AdminCoupon | null>(null);
  const [discountAmount, setDiscountAmount] = useState("");
  const [validFrom, setValidFrom] = useState("");
  const [validTo, setValidTo] = useState("");
  const [usageLimit, setUsageLimit] = useState("");
  const [active, setActive] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isNotFound, setIsNotFound] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getAdminCoupon(couponId)
      .then((c) => {
        setCoupon(c);
        setDiscountAmount(String(c.discountAmount));
        setValidFrom(toDateInputValue(c.validFrom));
        setValidTo(toDateInputValue(c.validTo));
        setUsageLimit(c.usageLimit != null ? String(c.usageLimit) : "");
        setActive(c.active);
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setIsNotFound(true);
        else setError(getErrorMessage(e, "クーポンの取得に失敗しました"));
      });
  }, [couponId]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!coupon) return;
    setError(null);
    setSubmitting(true);
    try {
      const updated = await updateAdminCoupon(couponId, {
        discountAmount: Number(discountAmount),
        validFrom: validFrom ? new Date(validFrom).toISOString() : null,
        validTo: validTo ? new Date(validTo).toISOString() : null,
        usageLimit: usageLimit ? Number(usageLimit) : null,
        active,
        version: coupon.version,
      });
      setCoupon(updated);
      showToast("クーポンを更新しました");
    } catch (err) {
      const message = getErrorMessage(
        err,
        "更新に失敗しました。画面を更新して再度お試しください",
      );
      setError(message);
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  }

  if (isNotFound) notFound();

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (!coupon) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <Link href="/admin/coupons" className="back-link">
        <ArrowLeft size={14} />
        クーポン一覧に戻る
      </Link>
      <div className="form-card">
        <h1>クーポン編集: {coupon.code}</h1>
        <p className="card-desc" style={{ marginBottom: 16 }}>
          利用回数: {coupon.usageCount}
          {coupon.usageLimit != null ? ` / ${coupon.usageLimit}` : ""}
        </p>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="discountAmount">割引額（円）</label>
            <input
              id="discountAmount"
              type="number"
              min={1}
              required
              value={discountAmount}
              onChange={(e) => setDiscountAmount(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="validFrom">有効期間開始（任意）</label>
            <input
              id="validFrom"
              type="date"
              value={validFrom}
              onChange={(e) => setValidFrom(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="validTo">有効期間終了（任意）</label>
            <input
              id="validTo"
              type="date"
              value={validTo}
              onChange={(e) => setValidTo(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="usageLimit">利用回数上限（任意、全体）</label>
            <input
              id="usageLimit"
              type="number"
              min={1}
              value={usageLimit}
              onChange={(e) => setUsageLimit(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 24 }}>
            <label>
              <input
                type="checkbox"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                style={{ marginRight: 8 }}
              />
              有効
            </label>
          </div>
          <button type="submit" disabled={submitting}>
            {submitting ? "更新中..." : "更新"}
          </button>
        </form>
      </div>
    </main>
  );
}
