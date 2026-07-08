"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  Package,
  Plus,
  PencilLine,
  Trash2,
  Search,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import {
  getAdminProducts,
  deleteAdminProduct,
  type AdminProduct,
} from "@/lib/api/adminProducts";
import {
  getAdminCategories,
  getProductCategories,
  type AdminCategory,
} from "@/lib/api/adminCategories";
import ConfirmModal from "../ConfirmModal";
import { useToast } from "@/app/Toast";

const PAGE_SIZE = 9;

type SortKey = "name" | "price-asc" | "price-desc" | "stock" | "updated";

function stockStatus(stock: number | undefined) {
  const s = stock ?? 0;
  if (s <= 0) return { cls: "out-stock", label: "在庫切れ" };
  if (s < 10) return { cls: "low-stock", label: "残りわずか" };
  return { cls: "in-stock", label: "在庫あり" };
}

function formatDate(value: string | undefined) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("ja-JP");
}

export default function AdminProductsPage() {
  const { showToast } = useToast();
  const [products, setProducts] = useState<AdminProduct[] | null>(null);
  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [categoriesByProduct, setCategoriesByProduct] = useState<
    Record<number, AdminCategory[]>
  >({});
  const [error, setError] = useState<string | null>(null);

  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [inStockOnly, setInStockOnly] = useState(false);
  const [sortBy, setSortBy] = useState<SortKey>("name");
  const [page, setPage] = useState(1);

  const filterKey = `${search}|${categoryFilter}|${inStockOnly}|${sortBy}`;
  const [prevFilterKey, setPrevFilterKey] = useState(filterKey);
  if (filterKey !== prevFilterKey) {
    setPrevFilterKey(filterKey);
    setPage(1);
  }

  const [deleteTarget, setDeleteTarget] = useState<AdminProduct | null>(null);

  useEffect(() => {
    getAdminProducts()
      .then(async (list) => {
        setProducts(list);
        const entries = await Promise.all(
          list.map(async (p) => {
            try {
              const cats = await getProductCategories(p.id!);
              return [p.id!, cats] as const;
            } catch {
              return [p.id!, []] as const;
            }
          }),
        );
        setCategoriesByProduct(Object.fromEntries(entries));
      })
      .catch(() => setError("商品一覧の取得に失敗しました"));
    getAdminCategories()
      .then(setCategories)
      .catch(() => {});
  }, []);

  const filtered = useMemo(() => {
    if (!products) return [];
    const term = search.trim().toLowerCase();
    let list = products.filter((p) => {
      const matchesTerm =
        !term ||
        p.name?.toLowerCase().includes(term) ||
        p.description?.toLowerCase().includes(term);
      const matchesCategory =
        !categoryFilter ||
        (categoriesByProduct[p.id!] ?? []).some(
          (c) => String(c.id) === categoryFilter,
        );
      const matchesStock = !inStockOnly || (p.stock ?? 0) > 0;
      return matchesTerm && matchesCategory && matchesStock;
    });

    list = [...list].sort((a, b) => {
      switch (sortBy) {
        case "price-asc":
          return (a.price ?? 0) - (b.price ?? 0);
        case "price-desc":
          return (b.price ?? 0) - (a.price ?? 0);
        case "stock":
          return (b.stock ?? 0) - (a.stock ?? 0);
        case "updated":
          return (
            new Date(b.updatedAt ?? 0).getTime() -
            new Date(a.updatedAt ?? 0).getTime()
          );
        default:
          return (a.name ?? "").localeCompare(b.name ?? "", "ja");
      }
    });

    return list;
  }, [
    products,
    search,
    categoryFilter,
    inStockOnly,
    sortBy,
    categoriesByProduct,
  ]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const paged = filtered.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  async function handleDelete() {
    if (!deleteTarget) return;
    try {
      await deleteAdminProduct(deleteTarget.id!);
      setProducts((prev) => prev!.filter((p) => p.id !== deleteTarget.id));
      showToast(`「${deleteTarget.name}」を削除しました`);
    } catch {
      showToast("削除に失敗しました", "error");
    } finally {
      setDeleteTarget(null);
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (products === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <div className="page-heading">
        <h1>
          <Package size={22} />
          商品管理
        </h1>
        <span className="page-count">全 {products.length} 件</span>
      </div>

      <Link href="/admin/products/new" className="btn-primary">
        <Plus size={16} />
        新規商品
      </Link>

      <div className="toolbar">
        <div className="search-wrap">
          <Search size={16} />
          <input
            type="search"
            placeholder="商品名・説明で検索"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <select
          value={categoryFilter}
          onChange={(e) => setCategoryFilter(e.target.value)}
        >
          <option value="">すべてのカテゴリー</option>
          {categories.map((c) => (
            <option key={c.id} value={String(c.id)}>
              {c.name}
            </option>
          ))}
        </select>
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value as SortKey)}
        >
          <option value="name">名前順</option>
          <option value="price-asc">価格が安い順</option>
          <option value="price-desc">価格が高い順</option>
          <option value="stock">在庫が多い順</option>
          <option value="updated">更新が新しい順</option>
        </select>
        <label className="toolbar-checkbox">
          <input
            type="checkbox"
            checked={inStockOnly}
            onChange={(e) => setInStockOnly(e.target.checked)}
          />
          在庫ありのみ
        </label>
      </div>

      {filtered.length === 0 ? (
        <p className="empty-state">条件に一致する商品がありません</p>
      ) : (
        <>
          <ul className="admin-list">
            {paged.map((product) => {
              const status = stockStatus(product.stock);
              const tags = categoriesByProduct[product.id!] ?? [];
              return (
                <li key={product.id}>
                  <div className="card-head">
                    <div
                      style={{
                        display: "flex",
                        gap: 10,
                        alignItems: "center",
                      }}
                    >
                      <span className="card-icon">
                        <Package size={18} />
                      </span>
                      <Link href={`/admin/products/${product.id}`}>
                        {product.name}
                      </Link>
                    </div>
                    <span className="card-updated">
                      更新: {formatDate(product.updatedAt)}
                    </span>
                  </div>

                  {product.description && (
                    <p className="card-desc">{product.description}</p>
                  )}

                  {tags.length > 0 && (
                    <div className="card-tags">
                      {tags.map((c) => (
                        <span key={c.id} className="card-tag">
                          {c.name}
                        </span>
                      ))}
                    </div>
                  )}

                  <div className="card-divider" />

                  <div className="card-footer">
                    <div>
                      <span
                        className="item-price"
                        style={{ display: "block", textAlign: "left" }}
                      >
                        ¥{product.price?.toLocaleString()}
                      </span>
                      <span className="stock-indicator">
                        <span className={`stock-dot ${status.cls}`} />
                        {status.label}（{product.stock}）
                      </span>
                    </div>
                    <span className="item-actions">
                      <Link
                        href={`/admin/products/${product.id}`}
                        className="icon-btn"
                        title="編集"
                      >
                        <PencilLine size={16} />
                      </Link>
                      <button
                        className="icon-btn icon-btn-danger"
                        title="削除"
                        onClick={() => setDeleteTarget(product)}
                      >
                        <Trash2 size={16} />
                      </button>
                    </span>
                  </div>
                </li>
              );
            })}
          </ul>

          <div className="pagination">
            <span className="page-count">
              {(currentPage - 1) * PAGE_SIZE + 1}〜
              {Math.min(currentPage * PAGE_SIZE, filtered.length)} 件 / 全
              {filtered.length} 件
            </span>
            <div className="page-buttons">
              <button
                className="page-btn"
                disabled={currentPage <= 1}
                onClick={() => setPage((p) => p - 1)}
              >
                <ChevronLeft size={16} />
              </button>
              {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
                <button
                  key={n}
                  className={"page-btn" + (n === currentPage ? " active" : "")}
                  onClick={() => setPage(n)}
                >
                  {n}
                </button>
              ))}
              <button
                className="page-btn"
                disabled={currentPage >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </>
      )}

      <ConfirmModal
        open={deleteTarget !== null}
        title="商品を削除しますか？"
        message={`「${deleteTarget?.name}」を削除します。この操作は取り消せません。`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </main>
  );
}
