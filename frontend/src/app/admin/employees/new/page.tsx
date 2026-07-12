"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import {
  createAdminEmployee,
  EMPLOYEE_ROLES,
  type EmployeeRole,
} from "@/lib/api/adminEmployees";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

export default function NewAdminEmployeePage() {
  const router = useRouter();
  const { showToast } = useToast();
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
      showToast("従業員を作成しました");
      router.push("/admin/employees");
    } catch (err) {
      const message = getErrorMessage(err, "従業員の作成に失敗しました");
      setError(message);
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <Link href="/admin/employees" className="back-link">
        <ArrowLeft size={14} />
        従業員一覧に戻る
      </Link>
      <div className="form-card">
        <h1>新規従業員作成</h1>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 16 }}>
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
          <div style={{ marginBottom: 16 }}>
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
          <div style={{ marginBottom: 24 }}>
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
          {error && <p style={{ color: "red", marginBottom: 16 }}>{error}</p>}
          <button type="submit" disabled={submitting}>
            {submitting ? "作成中..." : "作成"}
          </button>
        </form>
      </div>
    </main>
  );
}
