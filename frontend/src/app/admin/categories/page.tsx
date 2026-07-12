"use client";

import Link from "next/link";
import { FolderTree, Plus, PencilLine, Trash2 } from "lucide-react";
import {
  getAdminCategories,
  deleteAdminCategory,
  type AdminCategory,
} from "@/lib/api/adminCategories";
import ConfirmModal from "../ConfirmModal";
import { useAdminList } from "../useAdminList";

export default function AdminCategoriesPage() {
  const {
    items: categories,
    error,
    deleteTarget,
    setDeleteTarget,
    handleDelete,
  } = useAdminList<AdminCategory>({
    fetchList: getAdminCategories,
    deleteItem: deleteAdminCategory,
    getId: (c) => c.id!,
    loadErrorMessage: "カテゴリ一覧の取得に失敗しました",
    deleteSuccessMessage: (c) => `「${c.name}」を削除しました`,
  });

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (categories === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <div className="page-heading">
        <h1>
          <FolderTree size={22} />
          カテゴリ管理
        </h1>
        <span className="page-count">全 {categories.length} 件</span>
      </div>
      <Link href="/admin/categories/new" className="btn-primary">
        <Plus size={16} />
        新規カテゴリ
      </Link>
      <ul className="admin-list compact">
        {categories.map((category) => (
          <li key={category.id}>
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <span className="card-icon">
                <FolderTree size={18} />
              </span>
              <Link href={`/admin/categories/${category.id}`}>
                {category.name}
              </Link>
            </div>
            <span className="item-actions">
              <Link
                href={`/admin/categories/${category.id}`}
                className="icon-btn"
                title="編集"
              >
                <PencilLine size={16} />
              </Link>
              <button
                className="icon-btn icon-btn-danger"
                title="削除"
                onClick={() => setDeleteTarget(category)}
              >
                <Trash2 size={16} />
              </button>
            </span>
          </li>
        ))}
      </ul>

      <ConfirmModal
        open={deleteTarget !== null}
        title="カテゴリを削除しますか？"
        message={`「${deleteTarget?.name}」を削除します。この操作は取り消せません。`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </main>
  );
}
