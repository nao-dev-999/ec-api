"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  getCart,
  updateCartItemQuantity,
  removeCartItem,
  type CartItem,
} from "@/lib/api/cart";
import { createOrder } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/app/Toast";

export default function CartPage() {
  const router = useRouter();
  const { showToast } = useToast();
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
      setItems((prev) =>
        prev!.map((i) => (i.productId === updated.productId ? updated : i)),
      );
    } catch {
      showToast(
        "数量の更新に失敗しました。画面を更新して再度お試しください",
        "error",
      );
    }
  }

  async function handleRemove(productId: number) {
    try {
      await removeCartItem(productId);
      setItems((prev) => prev!.filter((i) => i.productId !== productId));
    } catch {
      showToast("削除に失敗しました", "error");
    }
  }

  async function handleCheckout() {
    if (!items || items.length === 0) return;
    setError(null);
    setPlacingOrder(true);
    try {
      const order = await createOrder({
        items: items.map((i) => ({
          productId: i.productId!,
          quantity: i.quantity!,
        })),
      });
      router.push(`/orders/${order.id}`);
    } catch {
      showToast("注文の作成に失敗しました", "error");
    } finally {
      setPlacingOrder(false);
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (items === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  const total = items.reduce((sum, i) => sum + (i.subtotal ?? 0), 0);

  return (
    <main>
      <h1>カート</h1>
      {items.length === 0 ? (
        <p>カートは空です</p>
      ) : (
        <>
          <ul>
            {items.map((item) => (
              <li key={item.productId}>
                <span>
                  {item.productName}{" "}
                  <span className="badge">¥{item.unitPrice}</span> ×{" "}
                  <input
                    type="number"
                    min={1}
                    value={item.quantity}
                    onChange={(e) =>
                      handleQuantityChange(item, Number(e.target.value))
                    }
                    style={{ width: 50 }}
                  />
                </span>
                <span
                  style={{ display: "flex", alignItems: "center", gap: 12 }}
                >
                  <span className="price">¥{item.subtotal}</span>
                  <button onClick={() => handleRemove(item.productId!)}>
                    削除
                  </button>
                </span>
              </li>
            ))}
          </ul>
          <p style={{ margin: "16px 0", textAlign: "right" }}>
            合計: <span className="price price-lg">¥{total}</span>
          </p>
          <button onClick={handleCheckout} disabled={placingOrder}>
            {placingOrder ? "注文中..." : "購入する"}
          </button>
        </>
      )}
    </main>
  );
}
