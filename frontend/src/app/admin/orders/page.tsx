"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getAdminOrders, type AdminOrder } from "@/lib/api/adminOrders";

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<AdminOrder[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminOrders()
      .then(setOrders)
      .catch(() => setError("注文一覧の取得に失敗しました"));
  }, []);

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (orders === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main style={{ padding: 24 }}>
      <h1>注文管理</h1>
      <ul>
        {orders.map((order) => (
          <li key={order.id} style={{ marginBottom: 8 }}>
            <Link href={`/admin/orders/${order.id}`}>
              注文番号 {order.id} — {order.customerName} — {order.status} — ¥{order.totalAmount}
            </Link>
          </li>
        ))}
      </ul>
    </main>
  );
}
