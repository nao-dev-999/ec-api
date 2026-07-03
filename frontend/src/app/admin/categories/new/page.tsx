"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createAdminCategory } from "@/lib/api/adminCategories";

export default function NewAdminCategoryPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await createAdminCategory({ name });
      router.push("/admin/categories");
    } catch {
      setError("カテゴリの作成に失敗しました");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main style={{ padding: 24, maxWidth: 400 }}>
      <h1>新規カテゴリ作成</h1>
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
        {error && <p style={{ color: "red" }}>{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? "作成中..." : "作成"}
        </button>
      </form>
    </main>
  );
}
