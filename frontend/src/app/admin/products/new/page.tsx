"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createAdminProduct } from "@/lib/api/adminProducts";

export default function NewAdminProductPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("0");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const product = await createAdminProduct({
        name,
        description,
        price: Number(price),
        stock: Number(stock),
      });
      router.push(`/admin/products/${product.id}`);
    } catch {
      setError("商品の作成に失敗しました");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main style={{ padding: 24, maxWidth: 400 }}>
      <h1>新規商品作成</h1>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 8 }}>
          <label htmlFor="name">商品名</label>
          <input
            id="name"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={{ display: "block", width: "100%" }}
          />
        </div>
        <div style={{ marginBottom: 8 }}>
          <label htmlFor="description">説明</label>
          <textarea
            id="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            style={{ display: "block", width: "100%" }}
          />
        </div>
        <div style={{ marginBottom: 8 }}>
          <label htmlFor="price">価格</label>
          <input
            id="price"
            type="number"
            min="0.01"
            step="0.01"
            required
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            style={{ display: "block", width: "100%" }}
          />
        </div>
        <div style={{ marginBottom: 8 }}>
          <label htmlFor="stock">在庫数</label>
          <input
            id="stock"
            type="number"
            min="0"
            required
            value={stock}
            onChange={(e) => setStock(e.target.value)}
            style={{ display: "block", width: "100%" }}
          />
        </div>
        {error && <p style={{ color: "red" }}>{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? "作成中..." : "作成"}
        </button>
      </form>
    </main>
  );
}
