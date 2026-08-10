"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, MapPin, Plus, PencilLine, Trash2 } from "lucide-react";
import {
  getShippingAddresses,
  deleteShippingAddress,
  type ShippingAddress,
} from "@/lib/api/shippingAddresses";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

export default function ShippingAddressesPage() {
  const router = useRouter();
  const { showToast } = useToast();
  const [addresses, setAddresses] = useState<ShippingAddress[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getShippingAddresses()
      .then(setAddresses)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.push("/login");
          return;
        }
        setError(getErrorMessage(err, "配送先住所の取得に失敗しました"));
      });
  }, [router]);

  async function handleDelete(address: ShippingAddress) {
    if (!confirm(`配送先「${address.recipientName}」を削除しますか？`)) return;
    try {
      await deleteShippingAddress(address.id);
      setAddresses((prev) => prev!.filter((a) => a.id !== address.id));
      showToast("配送先住所を削除しました");
    } catch (err) {
      showToast(getErrorMessage(err, "削除に失敗しました"), "error");
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (addresses === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <Link href="/mypage" className="back-link">
        <ArrowLeft size={14} />
        マイページに戻る
      </Link>
      <h1>
        <MapPin
          size={20}
          style={{ marginRight: 6, verticalAlign: "text-bottom" }}
        />
        配送先住所
      </h1>

      <Link
        href="/mypage/addresses/new"
        style={{
          display: "inline-flex",
          alignItems: "center",
          gap: 6,
          marginTop: 8,
        }}
      >
        <Plus size={16} />
        配送先を追加
      </Link>

      {addresses.length === 0 ? (
        <p style={{ marginTop: 16 }}>配送先住所はまだ登録されていません</p>
      ) : (
        <ul style={{ marginTop: 16 }}>
          {addresses.map((address) => (
            <li key={address.id}>
              <span>
                {address.recipientName} 様{" "}
                {address.isDefault && <span className="badge">既定</span>}
                <br />〒{address.postalCode} {address.prefecture}
                {address.city}
                {address.addressLine1}
                {address.addressLine2}
                <br />
                {address.phoneNumber}
              </span>
              <span style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <Link href={`/mypage/addresses/${address.id}`}>
                  <PencilLine size={14} />
                </Link>
                <button onClick={() => handleDelete(address)}>
                  <Trash2 size={14} />
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
