"use client";

import { use, useEffect, useState } from "react";
import {
  getAdminOrder,
  updateAdminOrderStatus,
  type AdminOrder,
  type OrderStatus,
} from "@/lib/api/adminOrders";

const STATUS_OPTIONS: OrderStatus[] = ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"];

export default function AdminOrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const orderId = Number(id);

  const [order, setOrder] = useState<AdminOrder | null>(null);
  const [status, setStatus] = useState<OrderStatus>("PENDING");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getAdminOrder(orderId)
      .then((o) => {
        setOrder(o);
        setStatus(o.status ?? "PENDING");
      })
      .catch(() => setError("注文の取得に失敗しました"));
  }, [orderId]);

  async function handleStatusChange(e: React.FormEvent) {
    e.preventDefault();
    if (!order) return;
    setError(null);
    setSubmitting(true);
    try {
      const updated = await updateAdminOrderStatus(orderId, status, order.version!);
      setOrder(updated);
    } catch {
      setError("ステータスの更新に失敗しました。画面を更新して再度お試しください");
    } finally {
      setSubmitting(false);
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (!order) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main style={{ padding: 24 }}>
      <h1>注文詳細</h1>
      <p>注文番号: {order.id}</p>
      <p>顧客: {order.customerName}</p>
      <ul>
        {order.items?.map((item) => (
          <li key={item.productId}>
            {item.productName} × {item.quantity} = ¥{item.subtotal}
          </li>
        ))}
      </ul>
      <p>合計: ¥{order.totalAmount}</p>

      <form onSubmit={handleStatusChange}>
        <label htmlFor="status">ステータス</label>
        <select
          id="status"
          value={status}
          onChange={(e) => setStatus(e.target.value as OrderStatus)}
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <button type="submit" disabled={submitting}>
          {submitting ? "更新中..." : "ステータスを更新"}
        </button>
      </form>
    </main>
  );
}
