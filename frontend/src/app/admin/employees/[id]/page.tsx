"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import {
  getAdminEmployee,
  updateAdminEmployeeRole,
  EMPLOYEE_ROLES,
  type AdminEmployee,
  type EmployeeRole,
} from "@/lib/api/adminEmployees";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

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
  const [isNotFound, setIsNotFound] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getAdminEmployee(employeeId)
      .then((e) => {
        setEmployee(e);
        setRole(e.role ?? EMPLOYEE_ROLES[0]);
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setIsNotFound(true);
        else setError(getErrorMessage(e, "従業員情報の取得に失敗しました"));
      });
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
    } catch (err) {
      const message = getErrorMessage(
        err,
        "更新に失敗しました。画面を更新して再度お試しください",
      );
      setError(message);
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  }

  if (isNotFound) notFound();

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
