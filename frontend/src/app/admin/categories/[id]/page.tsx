"use client";

import { use, useEffect, useState } from "react";
import {
  getAdminCategory,
  updateAdminCategory,
  type AdminCategory,
} from "@/lib/api/adminCategories";

export default function AdminCategoryDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const categoryId = Number(id);

  const [category, setCategory] = useState<AdminCategory | null>(null);
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getAdminCategory(categoryId)
      .then((c) => {
        setCategory(c);
        setName(c.name ?? "");
      })
      .catch(() => setError("カテゴリの取得に失敗しました"));
  }, [categoryId]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!category) return;
    setError(null);
    setSubmitting(true);
    try {
      const updated = await updateAdminCategory(categoryId, {
        name,
        version: category.version!,
      });
      setCategory(updated);
    } catch {
      setError("更新に失敗しました。画面を更新して再度お試しください");
    } finally {
      setSubmitting(false);
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (!category) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main style={{ padding: 24, maxWidth: 400 }}>
      <h1>カテゴリ編集</h1>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 8 }}>
          <label htmlFor="name">カテゴリ名</label>
          <input
            id="name"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={{ display: "block", width: "100%" }}
          />
        </div>
        <button type="submit" disabled={submitting}>
          {submitting ? "更新中..." : "更新"}
        </button>
      </form>
    </main>
  );
}
