"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { createAdminProduct } from "@/lib/api/adminProducts";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";
import { parsePrice, parseStock } from "@/lib/validation";

export default function NewAdminProductPage() {
  const router = useRouter();
  const { showToast } = useToast();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("0");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const priceValue = parsePrice(price);
    if (priceValue === null) {
      setError("価格は0より大きい数値を入力してください");
      return;
    }
    const stockValue = parseStock(stock);
    if (stockValue === null) {
      setError("在庫数は0以上の数値を入力してください");
      return;
    }

    setSubmitting(true);
    try {
      const product = await createAdminProduct({
        name,
        description,
        price: priceValue,
        stock: stockValue,
      });
      showToast("商品を作成しました");
      router.push(`/admin/products/${product.id}`);
    } catch (err) {
      const message = getErrorMessage(err, "商品の作成に失敗しました");
      setError(message);
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <Link href="/admin/products" className="back-link">
        <ArrowLeft size={14} />
        商品一覧に戻る
      </Link>
      <div className="form-card">
        <h1>新規商品作成</h1>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="name">商品名</label>
            <input
              id="name"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="description">説明</label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
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
          <div style={{ marginBottom: 24 }}>
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
          {error && <p style={{ color: "red", marginBottom: 16 }}>{error}</p>}
          <button type="submit" disabled={submitting}>
            {submitting ? "作成中..." : "作成"}
          </button>
        </form>
      </div>
    </main>
  );
}
