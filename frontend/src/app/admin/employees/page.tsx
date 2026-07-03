"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  getAdminEmployees,
  deleteAdminEmployee,
  type AdminEmployee,
} from "@/lib/api/adminEmployees";

export default function AdminEmployeesPage() {
  const [employees, setEmployees] = useState<AdminEmployee[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminEmployees()
      .then(setEmployees)
      .catch(() => setError("従業員一覧の取得に失敗しました"));
  }, []);

  async function handleDelete(id: number) {
    if (!confirm("この従業員を削除しますか？")) return;
    try {
      await deleteAdminEmployee(id);
      setEmployees((prev) => prev!.filter((e) => e.id !== id));
    } catch {
      setError("削除に失敗しました");
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (employees === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main style={{ padding: 24 }}>
      <h1>従業員管理</h1>
      <p>
        <Link href="/admin/employees/new">新規従業員を作成</Link>
      </p>
      <ul>
        {employees.map((employee) => (
          <li key={employee.id} style={{ marginBottom: 8 }}>
            <Link href={`/admin/employees/${employee.id}`}>
              {employee.email} — {employee.role}
            </Link>{" "}
            <button onClick={() => handleDelete(employee.id!)}>削除</button>
          </li>
        ))}
      </ul>
    </main>
  );
}
