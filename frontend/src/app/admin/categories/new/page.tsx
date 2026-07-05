"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { createAdminCategory } from "@/lib/api/adminCategories";
import { useToast } from "../../Toast";

export default function NewAdminCategoryPage() {
  const router = useRouter();
  const { showToast } = useToast();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await createAdminCategory({ name });
      showToast("カテゴリを作成しました");
      router.push("/admin/categories");
    } catch {
      setError("カテゴリの作成に失敗しました");
      showToast("カテゴリの作成に失敗しました", "error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <Link href="/admin/categories" className="back-link">
        <ArrowLeft size={14} />
        カテゴリ一覧に戻る
      </Link>
      <div className="form-card">
        <h1>新規カテゴリ作成</h1>
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
          {error && <p style={{ color: "red", marginBottom: 16 }}>{error}</p>}
          <button type="submit" disabled={submitting}>
            {submitting ? "作成中..." : "作成"}
          </button>
        </form>
      </div>
    </main>
  );
}
