"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import {
  getShippingAddress,
  updateShippingAddress,
  type ShippingAddress,
} from "@/lib/api/shippingAddresses";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

export default function ShippingAddressDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const addressId = Number(id);
  const { showToast } = useToast();

  const [address, setAddress] = useState<ShippingAddress | null>(null);
  const [recipientName, setRecipientName] = useState("");
  const [postalCode, setPostalCode] = useState("");
  const [prefecture, setPrefecture] = useState("");
  const [city, setCity] = useState("");
  const [addressLine1, setAddressLine1] = useState("");
  const [addressLine2, setAddressLine2] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [isDefault, setIsDefault] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isNotFound, setIsNotFound] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getShippingAddress(addressId)
      .then((a) => {
        setAddress(a);
        setRecipientName(a.recipientName);
        setPostalCode(a.postalCode);
        setPrefecture(a.prefecture);
        setCity(a.city);
        setAddressLine1(a.addressLine1);
        setAddressLine2(a.addressLine2 ?? "");
        setPhoneNumber(a.phoneNumber);
        setIsDefault(a.isDefault);
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setIsNotFound(true);
        else setError(getErrorMessage(e, "配送先住所の取得に失敗しました"));
      });
  }, [addressId]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!address) return;
    setError(null);
    setSubmitting(true);
    try {
      const updated = await updateShippingAddress(addressId, {
        recipientName,
        postalCode,
        prefecture,
        city,
        addressLine1,
        addressLine2,
        phoneNumber,
        isDefault,
        version: address.version,
      });
      setAddress(updated);
      showToast("配送先住所を更新しました");
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
  if (!address) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <Link href="/mypage/addresses" className="back-link">
        <ArrowLeft size={14} />
        配送先住所一覧に戻る
      </Link>
      <div className="form-card">
        <h1>配送先住所を編集</h1>
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
            {submitting ? "更新中..." : "更新"}
          </button>
        </form>
      </div>
    </main>
  );
}
