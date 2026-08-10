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
import { previewCoupon, type CouponPreview } from "@/lib/api/coupons";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";
import { parseQuantity } from "@/lib/validation";

export default function CartPage() {
  const router = useRouter();
  const { showToast } = useToast();
  const [items, setItems] = useState<CartItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [placingOrder, setPlacingOrder] = useState(false);
  const [couponCode, setCouponCode] = useState("");
  const [couponPreview, setCouponPreview] = useState<CouponPreview | null>(
    null,
  );
  const [couponChecking, setCouponChecking] = useState(false);
  const [couponMessage, setCouponMessage] = useState<string | null>(null);

  useEffect(() => {
    getCart()
      .then(setItems)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.push("/login");
          return;
        }
        setError(getErrorMessage(err, "カートの取得に失敗しました"));
      });
  }, [router]);

  async function handleQuantityChange(item: CartItem, quantity: number) {
    if (parseQuantity(quantity) === null) return;
    try {
      const updated = await updateCartItemQuantity(item.productId!, {
        quantity,
        version: item.version!,
      });
      setItems((prev) =>
        prev!.map((i) => (i.productId === updated.productId ? updated : i)),
      );
    } catch (err) {
      showToast(
        getErrorMessage(
          err,
          "数量の更新に失敗しました。画面を更新して再度お試しください",
        ),
        "error",
      );
    }
  }

  async function handleRemove(productId: number) {
    try {
      await removeCartItem(productId);
      setItems((prev) => prev!.filter((i) => i.productId !== productId));
    } catch (err) {
      showToast(getErrorMessage(err, "削除に失敗しました"), "error");
    }
  }

  async function handleCheckCoupon(subtotal: number) {
    const code = couponCode.trim();
    setCouponMessage(null);
    setCouponPreview(null);
    if (!code) return;
    setCouponChecking(true);
    try {
      const preview = await previewCoupon(code, subtotal);
      setCouponPreview(preview);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.push("/login");
        return;
      }
      setCouponMessage(getErrorMessage(err, "クーポンの確認に失敗しました"));
    } finally {
      setCouponChecking(false);
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
        couponCode: couponCode.trim() || undefined,
      });
      router.push(`/orders/${order.id}`);
    } catch (err) {
      showToast(getErrorMessage(err, "注文の作成に失敗しました"), "error");
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
          <div style={{ margin: "16px 0", textAlign: "right" }}>
            <label htmlFor="couponCode">クーポンコード（任意）: </label>
            <input
              id="couponCode"
              value={couponCode}
              onChange={(e) => {
                setCouponCode(e.target.value);
                setCouponPreview(null);
                setCouponMessage(null);
              }}
              style={{ width: 160, marginRight: 8 }}
            />
            <button
              type="button"
              onClick={() => handleCheckCoupon(total)}
              disabled={couponChecking || !couponCode.trim()}
            >
              {couponChecking ? "確認中..." : "確認"}
            </button>
            {couponMessage && (
              <p style={{ color: "red", marginTop: 8 }}>{couponMessage}</p>
            )}
            {couponPreview && (
              <p style={{ marginTop: 8 }}>
                クーポン「{couponPreview.code}」適用: -¥
                {couponPreview.discountAmount.toLocaleString()}
              </p>
            )}
          </div>
          {couponPreview ? (
            <>
              <p style={{ margin: "4px 0", textAlign: "right" }}>
                小計: ¥{total.toLocaleString()}
              </p>
              <p style={{ margin: "4px 0", textAlign: "right" }}>
                割引: -¥{couponPreview.discountAmount.toLocaleString()}
              </p>
              <p style={{ margin: "16px 0", textAlign: "right" }}>
                合計:{" "}
                <span className="price price-lg">
                  ¥{(total - couponPreview.discountAmount).toLocaleString()}
                </span>
              </p>
            </>
          ) : (
            <p style={{ margin: "16px 0", textAlign: "right" }}>
              合計: <span className="price price-lg">¥{total}</span>
            </p>
          )}
          <button onClick={handleCheckout} disabled={placingOrder}>
            {placingOrder ? "注文中..." : "購入する"}
          </button>
        </>
      )}
    </main>
  );
}
