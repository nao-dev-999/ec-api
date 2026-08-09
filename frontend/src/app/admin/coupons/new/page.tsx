"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { createAdminCoupon } from "@/lib/api/adminCoupons";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

export default function NewAdminCouponPage() {
  const router = useRouter();
  const { showToast } = useToast();
  const [code, setCode] = useState("");
  const [discountAmount, setDiscountAmount] = useState("");
  const [validFrom, setValidFrom] = useState("");
  const [validTo, setValidTo] = useState("");
  const [usageLimit, setUsageLimit] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await createAdminCoupon({
        code,
        discountAmount: Number(discountAmount),
        validFrom: validFrom ? new Date(validFrom).toISOString() : null,
        validTo: validTo ? new Date(validTo).toISOString() : null,
        usageLimit: usageLimit ? Number(usageLimit) : null,
      });
      showToast("クーポンを作成しました");
      router.push("/admin/coupons");
    } catch (err) {
      const message = getErrorMessage(err, "クーポンの作成に失敗しました");
      setError(message);
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <Link href="/admin/coupons" className="back-link">
        <ArrowLeft size={14} />
        クーポン一覧に戻る
      </Link>
      <div className="form-card">
        <h1>新規クーポン作成</h1>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="code">クーポンコード</label>
            <input
              id="code"
              required
              maxLength={30}
              value={code}
              onChange={(e) => setCode(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
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
          <div style={{ marginBottom: 24 }}>
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
          {error && <p style={{ color: "red", marginBottom: 16 }}>{error}</p>}
          <button type="submit" disabled={submitting}>
            {submitting ? "作成中..." : "作成"}
          </button>
        </form>
      </div>
    </main>
  );
}
