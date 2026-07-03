"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getOrder, type Order } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";

export default function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getOrder(Number(id))
      .then(setOrder)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.push("/login");
          return;
        }
        setError("注文の取得に失敗しました");
      });
  }, [id, router]);

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (!order) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main style={{ padding: 24 }}>
      <h1>ご注文ありがとうございました</h1>
      <p>注文番号: {order.id}</p>
      <p>ステータス: {order.status}</p>
      <ul>
        {order.items?.map((item) => (
          <li key={item.productId}>
            {item.productName} × {item.quantity} = ¥{item.subtotal}
          </li>
        ))}
      </ul>
      <p>合計: ¥{order.totalAmount}</p>
    </main>
  );
}
