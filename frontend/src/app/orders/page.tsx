"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { getMyOrders, type Order } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";
import OrderStatusBadge from "../OrderStatusBadge";

const PAGE_SIZE = 10;

function formatDate(value: string | undefined) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("ja-JP");
}

export default function OrderHistoryPage() {
  const router = useRouter();
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getMyOrders(page - 1, PAGE_SIZE)
      .then((result) => {
        setOrders(result.content ?? []);
        setTotalPages(Math.max(1, result.totalPages ?? 1));
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.push("/login");
          return;
        }
        setError("注文履歴の取得に失敗しました");
      });
  }, [page, router]);

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (orders === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <h1>注文履歴</h1>
      {orders.length === 0 ? (
        <p>注文履歴はありません</p>
      ) : (
        <>
          <ul>
            {orders.map((order) => (
              <li key={order.id}>
                <Link href={`/orders/${order.id}`} style={{ flex: 1 }}>
                  <span style={{ display: "block", fontWeight: 600 }}>
                    注文番号 #{order.id}
                  </span>
                  <span
                    style={{
                      color: "var(--muted)",
                      fontSize: "0.85rem",
                    }}
                  >
                    {formatDate(order.orderedAt)}
                  </span>
                </Link>
                <OrderStatusBadge status={order.status} />
                <span className="price">¥{order.totalAmount}</span>
              </li>
            ))}
          </ul>
          {totalPages > 1 && (
            <div
              style={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                gap: 16,
                marginTop: 20,
              }}
            >
              <button
                onClick={() => setPage((p) => p - 1)}
                disabled={page <= 1}
              >
                <ChevronLeft size={16} />
              </button>
              <span>
                {page} / {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages}
              >
                <ChevronRight size={16} />
              </button>
            </div>
          )}
        </>
      )}
    </main>
  );
}
