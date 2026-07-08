"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import {
  getAdminCategory,
  updateAdminCategory,
  type AdminCategory,
} from "@/lib/api/adminCategories";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/app/Toast";

export default function AdminCategoryDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const categoryId = Number(id);
  const { showToast } = useToast();

  const [category, setCategory] = useState<AdminCategory | null>(null);
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getAdminCategory(categoryId)
      .then((c) => {
        setCategory(c);
        setName(c.name ?? "");
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setNotFound(true);
        else setError("カテゴリの取得に失敗しました");
      });
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
      showToast("カテゴリを更新しました");
    } catch {
      setError("更新に失敗しました。画面を更新して再度お試しください");
      showToast("更新に失敗しました", "error");
    } finally {
      setSubmitting(false);
    }
  }

  if (notFound) {
    return (
      <main>
        <Link href="/admin/categories" className="back-link">
          <ArrowLeft size={14} />
          カテゴリ一覧に戻る
        </Link>
        <h1>カテゴリが見つかりません</h1>
        <p>指定されたカテゴリは存在しないか、削除された可能性があります。</p>
      </main>
    );
  }
  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (!category) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <Link href="/admin/categories" className="back-link">
        <ArrowLeft size={14} />
        カテゴリ一覧に戻る
      </Link>
      <div className="form-card">
        <h1>カテゴリ編集</h1>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 24 }}>
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
      </div>
    </main>
  );
}
