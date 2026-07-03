"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { getMyOrders, type Order } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";

export default function OrderHistoryPage() {
  const router = useRouter();
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getMyOrders()
      .then(setOrders)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.push("/login");
          return;
        }
        setError("注文履歴の取得に失敗しました");
      });
  }, [router]);

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (orders === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main style={{ padding: 24 }}>
      <h1>注文履歴</h1>
      {orders.length === 0 ? (
        <p>注文履歴はありません</p>
      ) : (
        <ul>
          {orders.map((order) => (
            <li key={order.id} style={{ marginBottom: 8 }}>
              <Link href={`/orders/${order.id}`}>
                注文番号 {order.id} — {order.status} — ¥{order.totalAmount}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
