"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import {
  getLowStockProducts,
  type AdminProduct,
} from "@/lib/api/adminProducts";
import { getErrorMessage } from "@/lib/errors/messages";

const DEFAULT_THRESHOLD = 10;

export default function LowStockAlertsPage() {
  const [threshold, setThreshold] = useState(DEFAULT_THRESHOLD);
  const [products, setProducts] = useState<AdminProduct[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getLowStockProducts(threshold)
      .then(setProducts)
      .catch((err) =>
        setError(getErrorMessage(err, "低在庫商品の取得に失敗しました")),
      );
  }, [threshold]);

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;

  return (
    <main>
      <div className="page-heading">
        <h1>
          <AlertTriangle size={22} />
          在庫アラート
        </h1>
        <span className="page-count">
          全 {products === null ? "-" : products.length} 件
        </span>
      </div>

      <div style={{ marginBottom: 16 }}>
        <label htmlFor="threshold">在庫閾値: </label>
        <input
          id="threshold"
          type="number"
          min={0}
          value={threshold}
          onChange={(e) =>
            setThreshold(Math.max(0, Number(e.target.value) || 0))
          }
          style={{ width: 80 }}
        />
        <span style={{ marginLeft: 8, color: "var(--muted)" }}>
          以下の商品を表示します
        </span>
      </div>

      {products === null ? (
        <p style={{ padding: 24 }}>読み込み中...</p>
      ) : (
        <ul className="admin-list">
          {products.length === 0 && (
            <p style={{ padding: "16px 0", color: "var(--muted)" }}>
              閾値以下の在庫の商品はありません
            </p>
          )}
          {products.map((product) => (
            <li key={product.id}>
              <div className="card-head">
                <div>
                  <Link href={`/admin/products/${product.id}`}>
                    {product.name}
                  </Link>
                  <p className="card-desc" style={{ marginTop: 2 }}>
                    ¥{product.price?.toLocaleString()}
                  </p>
                </div>
                <span className="badge" style={{ color: "#b91c1c" }}>
                  在庫: {product.stock}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
