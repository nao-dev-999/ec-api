"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, X } from "lucide-react";
import {
  getAdminProduct,
  updateAdminProduct,
  type AdminProduct,
} from "@/lib/api/adminProducts";
import {
  getAdminCategories,
  getProductCategories,
  addCategoryToProduct,
  removeCategoryFromProduct,
  type AdminCategory,
} from "@/lib/api/adminCategories";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/app/Toast";

export default function AdminProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const productId = Number(id);
  const { showToast } = useToast();

  const [product, setProduct] = useState<AdminProduct | null>(null);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [allCategories, setAllCategories] = useState<AdminCategory[]>([]);
  const [productCategories, setProductCategories] = useState<AdminCategory[]>(
    [],
  );
  const [selectedCategoryId, setSelectedCategoryId] = useState("");

  useEffect(() => {
    getAdminProduct(productId)
      .then((p) => {
        setProduct(p);
        setName(p.name ?? "");
        setDescription(p.description ?? "");
        setPrice(String(p.price ?? ""));
        setStock(String(p.stock ?? ""));
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setNotFound(true);
        else setError("商品の取得に失敗しました");
      });
    getAdminCategories()
      .then(setAllCategories)
      .catch(() => showToast("カテゴリ一覧の取得に失敗しました", "error"));
    getProductCategories(productId)
      .then(setProductCategories)
      .catch(() => showToast("設定済みカテゴリの取得に失敗しました", "error"));
  }, [productId, showToast]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!product) return;
    setError(null);
    setSubmitting(true);
    try {
      const updated = await updateAdminProduct(productId, {
        id: productId,
        name,
        description,
        price: Number(price),
        stock: Number(stock),
        version: product.version!,
      });
      setProduct(updated);
      showToast("商品を更新しました");
    } catch {
      setError("更新に失敗しました。画面を更新して再度お試しください");
      showToast("更新に失敗しました", "error");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAddCategory() {
    if (!selectedCategoryId) return;
    try {
      await addCategoryToProduct(productId, Number(selectedCategoryId));
      const updated = await getProductCategories(productId);
      setProductCategories(updated);
      setSelectedCategoryId("");
    } catch {
      showToast("カテゴリの追加に失敗しました", "error");
    }
  }

  async function handleRemoveCategory(categoryId: number) {
    try {
      await removeCategoryFromProduct(productId, categoryId);
      setProductCategories((prev) => prev.filter((c) => c.id !== categoryId));
    } catch {
      showToast("カテゴリの削除に失敗しました", "error");
    }
  }

  if (notFound) {
    return (
      <main>
        <Link href="/admin/products" className="back-link">
          <ArrowLeft size={14} />
          商品一覧に戻る
        </Link>
        <h1>商品が見つかりません</h1>
        <p>指定された商品は存在しないか、削除された可能性があります。</p>
      </main>
    );
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (!product) return <p style={{ padding: 24 }}>読み込み中...</p>;

  const unassignedCategories = allCategories.filter(
    (c) => !productCategories.some((pc) => pc.id === c.id),
  );

  return (
    <main>
      <Link href="/admin/products" className="back-link">
        <ArrowLeft size={14} />
        商品一覧に戻る
      </Link>
      <div className="form-card">
        <h1>商品編集</h1>
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
          <button type="submit" disabled={submitting}>
            {submitting ? "更新中..." : "更新"}
          </button>
        </form>

        <section style={{ marginTop: 32 }}>
          <h2>カテゴリ</h2>
          {productCategories.length > 0 && (
            <div className="card-tags" style={{ marginBottom: 16 }}>
              {productCategories.map((c) => (
                <span key={c.id} className="card-tag">
                  {c.name}
                  <button
                    onClick={() => handleRemoveCategory(c.id!)}
                    title="削除"
                    style={{
                      background: "transparent",
                      color: "inherit",
                      padding: 0,
                      marginLeft: 6,
                      display: "inline-flex",
                    }}
                  >
                    <X size={12} />
                  </button>
                </span>
              ))}
            </div>
          )}
          <div style={{ display: "flex", gap: 8 }}>
            <select
              value={selectedCategoryId}
              onChange={(e) => setSelectedCategoryId(e.target.value)}
              style={{ flex: 1 }}
            >
              <option value="">カテゴリを選択</option>
              {unassignedCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            <button onClick={handleAddCategory}>追加</button>
          </div>
        </section>
      </div>
    </main>
  );
}
