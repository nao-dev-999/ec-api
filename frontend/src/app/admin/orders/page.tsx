"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Receipt, Eye, ChevronLeft, ChevronRight } from "lucide-react";
import { getAdminOrders, type AdminOrder } from "@/lib/api/adminOrders";
import OrderStatusBadge from "../../OrderStatusBadge";
import { getErrorMessage } from "@/lib/errors/messages";

const PAGE_SIZE = 20;

function formatDate(value: string | undefined) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("ja-JP");
}

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<AdminOrder[] | null>(null);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminOrders(page - 1, PAGE_SIZE)
      .then((result) => {
        setOrders(result.content ?? []);
        setTotalPages(Math.max(1, result.totalPages ?? 1));
        setTotalElements(result.totalElements ?? 0);
      })
      .catch((err) => setError(getErrorMessage(err, "注文一覧の取得に失敗しました")));
  }, [page]);

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (orders === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <div className="page-heading">
        <h1>
          <Receipt size={22} />
          注文管理
        </h1>
        <span className="page-count">全 {totalElements} 件</span>
      </div>
      <ul className="admin-list">
        {orders.map((order) => (
          <li key={order.id}>
            <div className="card-head">
              <div>
                <Link href={`/admin/orders/${order.id}`}>
                  注文番号 #{order.id}
                </Link>
                <p className="card-desc" style={{ marginTop: 2 }}>
                  {order.customerName}
                </p>
              </div>
              <span className="card-updated">
                {formatDate(order.orderedAt)}
              </span>
            </div>

            <OrderStatusBadge status={order.status} />

            <div className="card-divider" />

            <div className="card-footer">
              <span className="item-price" style={{ textAlign: "left" }}>
                ¥{order.totalAmount?.toLocaleString()}
              </span>
              <span className="item-actions">
                <Link
                  href={`/admin/orders/${order.id}`}
                  className="icon-btn"
                  title="詳細"
                >
                  <Eye size={16} />
                </Link>
              </span>
            </div>
          </li>
        ))}
      </ul>

      <div className="pagination">
        <span className="page-count">
          {orders.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1}〜
          {Math.min(page * PAGE_SIZE, totalElements)} 件 / 全{totalElements} 件
        </span>
        <div className="page-buttons">
          <button
            className="page-btn"
            disabled={page <= 1}
            onClick={() => setPage((p) => p - 1)}
          >
            <ChevronLeft size={16} />
          </button>
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
            <button
              key={n}
              className={"page-btn" + (n === page ? " active" : "")}
              onClick={() => setPage(n)}
            >
              {n}
            </button>
          ))}
          <button
            className="page-btn"
            disabled={page >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            <ChevronRight size={16} />
          </button>
        </div>
      </div>
    </main>
  );
}
