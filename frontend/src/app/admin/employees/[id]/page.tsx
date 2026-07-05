"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import {
  getAdminEmployee,
  updateAdminEmployeeRole,
  EMPLOYEE_ROLES,
  type AdminEmployee,
  type EmployeeRole,
} from "@/lib/api/adminEmployees";
import { useToast } from "../../Toast";

export default function AdminEmployeeDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const employeeId = Number(id);
  const { showToast } = useToast();

  const [employee, setEmployee] = useState<AdminEmployee | null>(null);
  const [role, setRole] = useState<EmployeeRole>(EMPLOYEE_ROLES[0]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getAdminEmployee(employeeId)
      .then((e) => {
        setEmployee(e);
        setRole(e.role ?? EMPLOYEE_ROLES[0]);
      })
      .catch(() => setError("従業員情報の取得に失敗しました"));
  }, [employeeId]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!employee) return;
    setError(null);
    setSubmitting(true);
    try {
      const updated = await updateAdminEmployeeRole(employeeId, {
        role,
        version: employee.version!,
      });
      setEmployee(updated);
      showToast("従業員情報を更新しました");
    } catch {
      setError("更新に失敗しました。画面を更新して再度お試しください");
      showToast("更新に失敗しました", "error");
    } finally {
      setSubmitting(false);
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (!employee) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <Link href="/admin/employees" className="back-link">
        <ArrowLeft size={14} />
        従業員一覧に戻る
      </Link>
      <div className="form-card">
        <h1>従業員編集</h1>
        <p style={{ marginBottom: 24 }}>メールアドレス: {employee.email}</p>
        <form onSubmit={handleSubmit}>
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
          <button type="submit" disabled={submitting}>
            {submitting ? "更新中..." : "ロールを更新"}
          </button>
        </form>
      </div>
    </main>
  );
}
