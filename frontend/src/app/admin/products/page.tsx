"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getAdminProducts, deleteAdminProduct, type AdminProduct } from "@/lib/api/adminProducts";

export default function AdminProductsPage() {
  const [products, setProducts] = useState<AdminProduct[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminProducts()
      .then(setProducts)
      .catch(() => setError("商品一覧の取得に失敗しました"));
  }, []);

  async function handleDelete(id: number) {
    if (!confirm("この商品を削除しますか？")) return;
    try {
      await deleteAdminProduct(id);
      setProducts((prev) => prev!.filter((p) => p.id !== id));
    } catch {
      setError("削除に失敗しました");
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (products === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main style={{ padding: 24 }}>
      <h1>商品管理</h1>
      <p>
        <Link href="/admin/products/new">新規商品を作成</Link>
      </p>
      <ul>
        {products.map((product) => (
          <li key={product.id} style={{ marginBottom: 8 }}>
            <Link href={`/admin/products/${product.id}`}>
              {product.name} — ¥{product.price} (在庫: {product.stock})
            </Link>{" "}
            <button onClick={() => handleDelete(product.id!)}>削除</button>
          </li>
        ))}
      </ul>
    </main>
  );
}
