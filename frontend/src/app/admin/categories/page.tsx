"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  getAdminCategories,
  deleteAdminCategory,
  type AdminCategory,
} from "@/lib/api/adminCategories";

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<AdminCategory[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminCategories()
      .then(setCategories)
      .catch(() => setError("カテゴリ一覧の取得に失敗しました"));
  }, []);

  async function handleDelete(id: number) {
    if (!confirm("このカテゴリを削除しますか？")) return;
    try {
      await deleteAdminCategory(id);
      setCategories((prev) => prev!.filter((c) => c.id !== id));
    } catch {
      setError("削除に失敗しました");
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (categories === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main style={{ padding: 24 }}>
      <h1>カテゴリ管理</h1>
      <p>
        <Link href="/admin/categories/new">新規カテゴリを作成</Link>
      </p>
      <ul>
        {categories.map((category) => (
          <li key={category.id} style={{ marginBottom: 8 }}>
            <Link href={`/admin/categories/${category.id}`}>{category.name}</Link>{" "}
            <button onClick={() => handleDelete(category.id!)}>削除</button>
          </li>
        ))}
      </ul>
    </main>
  );
}
