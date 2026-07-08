"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import {
  getAdminOrder,
  updateAdminOrderStatus,
  type AdminOrder,
  type OrderStatus,
} from "@/lib/api/adminOrders";
import { ApiError } from "@/lib/api/client";
import OrderStatusBadge from "../../../OrderStatusBadge";
import { useToast } from "@/app/Toast";

const STATUS_OPTIONS: OrderStatus[] = [
  "PENDING",
  "CONFIRMED",
  "SHIPPED",
  "DELIVERED",
  "CANCELLED",
];

function formatDate(value: string | undefined) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("ja-JP");
}

export default function AdminOrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const orderId = Number(id);
  const { showToast } = useToast();

  const [order, setOrder] = useState<AdminOrder | null>(null);
  const [status, setStatus] = useState<OrderStatus>("PENDING");
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getAdminOrder(orderId)
      .then((o) => {
        setOrder(o);
        setStatus(o.status ?? "PENDING");
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setNotFound(true);
        else setError("注文の取得に失敗しました");
      });
  }, [orderId]);

  async function handleStatusChange(e: React.FormEvent) {
    e.preventDefault();
    if (!order) return;
    setError(null);
    setSubmitting(true);
    try {
      const updated = await updateAdminOrderStatus(
        orderId,
        status,
        order.version!,
      );
      setOrder(updated);
      showToast("ステータスを更新しました");
    } catch {
      setError(
        "ステータスの更新に失敗しました。画面を更新して再度お試しください",
      );
      showToast("ステータスの更新に失敗しました", "error");
    } finally {
      setSubmitting(false);
    }
  }

  if (notFound) {
    return (
      <main>
        <Link href="/admin/orders" className="back-link">
          <ArrowLeft size={14} />
          注文一覧に戻る
        </Link>
        <h1>注文が見つかりません</h1>
        <p>指定された注文は存在しないか、削除された可能性があります。</p>
      </main>
    );
  }
  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (!order) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <Link href="/admin/orders" className="back-link">
        <ArrowLeft size={14} />
        注文一覧に戻る
      </Link>
      <div className="form-card">
        <h1>注文詳細</h1>
        <p>注文番号: #{order.id}</p>
        <p>顧客: {order.customerName}</p>
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

        <form onSubmit={handleStatusChange} style={{ marginTop: 24 }}>
          <div style={{ marginBottom: 16 }}>
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
          </div>
          <button type="submit" disabled={submitting}>
            {submitting ? "更新中..." : "ステータスを更新"}
          </button>
        </form>
      </div>
    </main>
  );
}
