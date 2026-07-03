"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  createAdminEmployee,
  EMPLOYEE_ROLES,
  type EmployeeRole,
} from "@/lib/api/adminEmployees";

export default function NewAdminEmployeePage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<EmployeeRole>(EMPLOYEE_ROLES[0]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await createAdminEmployee({ email, password, role });
      router.push("/admin/employees");
    } catch {
      setError("従業員の作成に失敗しました");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main style={{ padding: 24, maxWidth: 400 }}>
      <h1>新規従業員作成</h1>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 8 }}>
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
        <div style={{ marginBottom: 8 }}>
          <label htmlFor="password">パスワード（8文字以上）</label>
          <input
            id="password"
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={{ display: "block", width: "100%" }}
          />
        </div>
        <div style={{ marginBottom: 8 }}>
          <label htmlFor="role">ロール</label>
          <select
            id="role"
            value={role}
            onChange={(e) => setRole(e.target.value as EmployeeRole)}
            style={{ display: "block", width: "100%" }}
          >
            {EMPLOYEE_ROLES.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>
        {error && <p style={{ color: "red" }}>{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? "作成中..." : "作成"}
        </button>
      </form>
    </main>
  );
}
