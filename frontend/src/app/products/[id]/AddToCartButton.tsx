"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { addCartItem } from "@/lib/api/cart";
import { ApiError } from "@/lib/api/client";

export default function AddToCartButton({ productId }: { productId: number }) {
  const router = useRouter();
  const [quantity, setQuantity] = useState(1);
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleAdd() {
    setMessage(null);
    setSubmitting(true);
    try {
      await addCartItem({ productId, quantity });
      setMessage("カートに追加しました");
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.push("/login");
        return;
      }
      setMessage("カートへの追加に失敗しました");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ marginTop: 16 }}>
      <label htmlFor="quantity">数量: </label>
      <input
        id="quantity"
        type="number"
        min={1}
        value={quantity}
        onChange={(e) => setQuantity(Number(e.target.value))}
        style={{ width: 60, marginRight: 12 }}
      />
      <button onClick={handleAdd} disabled={submitting}>
        {submitting ? "追加中..." : "カートに追加"}
      </button>
      {message && <p>{message}</p>}
    </div>
  );
}
