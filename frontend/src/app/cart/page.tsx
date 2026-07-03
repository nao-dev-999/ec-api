"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getCart, updateCartItemQuantity, removeCartItem, type CartItem } from "@/lib/api/cart";
import { createOrder } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";

export default function CartPage() {
  const router = useRouter();
  const [items, setItems] = useState<CartItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [placingOrder, setPlacingOrder] = useState(false);

  useEffect(() => {
    getCart()
      .then(setItems)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.push("/login");
          return;
        }
        setError("カートの取得に失敗しました");
      });
  }, [router]);

  async function handleQuantityChange(item: CartItem, quantity: number) {
    if (quantity < 1) return;
    try {
      const updated = await updateCartItemQuantity(item.productId!, {
        quantity,
        version: item.version!,
      });
      setItems((prev) => prev!.map((i) => (i.productId === updated.productId ? updated : i)));
    } catch {
      setError("数量の更新に失敗しました。画面を更新して再度お試しください");
    }
  }

  async function handleRemove(productId: number) {
    try {
      await removeCartItem(productId);
      setItems((prev) => prev!.filter((i) => i.productId !== productId));
    } catch {
      setError("削除に失敗しました");
    }
  }

  async function handleCheckout() {
    if (!items || items.length === 0) return;
    setError(null);
    setPlacingOrder(true);
    try {
      const order = await createOrder({
        items: items.map((i) => ({ productId: i.productId!, quantity: i.quantity! })),
      });
      router.push(`/orders/${order.id}`);
    } catch {
      setError("注文の作成に失敗しました");
    } finally {
      setPlacingOrder(false);
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (items === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  const total = items.reduce((sum, i) => sum + (i.subtotal ?? 0), 0);

  return (
    <main style={{ padding: 24 }}>
      <h1>カート</h1>
      {items.length === 0 ? (
        <p>カートは空です</p>
      ) : (
        <>
          <ul>
            {items.map((item) => (
              <li key={item.productId} style={{ marginBottom: 8 }}>
                {item.productName} — ¥{item.unitPrice} ×{" "}
                <input
                  type="number"
                  min={1}
                  value={item.quantity}
                  onChange={(e) => handleQuantityChange(item, Number(e.target.value))}
                  style={{ width: 50 }}
                />
                {" "}= ¥{item.subtotal}{" "}
                <button onClick={() => handleRemove(item.productId!)}>削除</button>
              </li>
            ))}
          </ul>
          <p>合計: ¥{total}</p>
          <button onClick={handleCheckout} disabled={placingOrder}>
            {placingOrder ? "注文中..." : "注文する"}
          </button>
        </>
      )}
    </main>
  );
}
