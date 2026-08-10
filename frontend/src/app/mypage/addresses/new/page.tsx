"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { createShippingAddress } from "@/lib/api/shippingAddresses";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

export default function NewShippingAddressPage() {
  const router = useRouter();
  const { showToast } = useToast();
  const [recipientName, setRecipientName] = useState("");
  const [postalCode, setPostalCode] = useState("");
  const [prefecture, setPrefecture] = useState("");
  const [city, setCity] = useState("");
  const [addressLine1, setAddressLine1] = useState("");
  const [addressLine2, setAddressLine2] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [isDefault, setIsDefault] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await createShippingAddress({
        recipientName,
        postalCode,
        prefecture,
        city,
        addressLine1,
        addressLine2: addressLine2.trim() || undefined,
        phoneNumber,
        isDefault,
      });
      showToast("配送先住所を登録しました");
      router.push("/mypage/addresses");
    } catch (err) {
      const message = getErrorMessage(err, "配送先住所の登録に失敗しました");
      setError(message);
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <Link href="/mypage/addresses" className="back-link">
        <ArrowLeft size={14} />
        配送先住所一覧に戻る
      </Link>
      <div className="form-card">
        <h1>配送先住所を追加</h1>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="recipientName">お届け先氏名</label>
            <input
              id="recipientName"
              required
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="postalCode">郵便番号</label>
            <input
              id="postalCode"
              required
              placeholder="100-0001"
              value={postalCode}
              onChange={(e) => setPostalCode(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="prefecture">都道府県</label>
            <input
              id="prefecture"
              required
              value={prefecture}
              onChange={(e) => setPrefecture(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="city">市区町村</label>
            <input
              id="city"
              required
              value={city}
              onChange={(e) => setCity(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="addressLine1">番地</label>
            <input
              id="addressLine1"
              required
              value={addressLine1}
              onChange={(e) => setAddressLine1(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="addressLine2">建物名・部屋番号（任意）</label>
            <input
              id="addressLine2"
              value={addressLine2}
              onChange={(e) => setAddressLine2(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="phoneNumber">電話番号</label>
            <input
              id="phoneNumber"
              required
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 24 }}>
            <label>
              <input
                type="checkbox"
                checked={isDefault}
                onChange={(e) => setIsDefault(e.target.checked)}
                style={{ marginRight: 8 }}
              />
              既定の配送先にする
            </label>
          </div>
          {error && <p style={{ color: "red", marginBottom: 16 }}>{error}</p>}
          <button type="submit" disabled={submitting}>
            {submitting ? "登録中..." : "登録"}
          </button>
        </form>
      </div>
    </main>
  );
}
