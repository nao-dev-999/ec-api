"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { getOrder, type Order } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";
import OrderStatusBadge from "../../OrderStatusBadge";

function formatDate(value: string | undefined) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("ja-JP");
}

export default function OrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
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
    <main>
      <Link href="/orders" className="back-link">
        <ArrowLeft size={14} />
        注文履歴に戻る
      </Link>
      <div className="form-card">
        <h1>ご注文ありがとうございました</h1>
        <p>注文番号: #{order.id}</p>
        <p style={{ margin: "8px 0 16px" }}>
          注文日: {formatDate(order.orderedAt)}
        </p>
        <OrderStatusBadge status={order.status} />

        <table className="order-table">
          <thead>
            <tr>
              <th>商品名</th>
              <th className="num">数量</th>
              <th className="num">単価</th>
              <th className="num">小計</th>
            </tr>
          </thead>
          <tbody>
            {order.items?.map((item) => (
              <tr key={item.productId}>
                <td>{item.productName}</td>
                <td className="num">{item.quantity}</td>
                <td className="num">¥{item.unitPrice?.toLocaleString()}</td>
                <td className="num">¥{item.subtotal?.toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={3}>合計</td>
              <td className="num">¥{order.totalAmount?.toLocaleString()}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </main>
  );
}
