"use client";

import { useState } from "react";
import { getErrorMessage } from "@/lib/errors/messages";

export default function LoginForm({
  title,
  onLogin,
  style,
}: {
  title: string;
  onLogin: (email: string, password: string) => Promise<void>;
  style?: React.CSSProperties;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onLogin(email, password);
    } catch (err) {
      setError(getErrorMessage(err, "ログインに失敗しました"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main style={style}>
      <h1>{title}</h1>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="email">メールアドレス</label>
          <input
            id="email"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={{ display: "block", width: "100%" }}
          />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="password">パスワード</label>
          <input
            id="password"
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={{ display: "block", width: "100%" }}
          />
        </div>
        {error && <p style={{ color: "red" }}>{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? "ログイン中..." : "ログイン"}
        </button>
      </form>
    </main>
  );
}
