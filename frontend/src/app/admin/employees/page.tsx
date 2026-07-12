"use client";

import Link from "next/link";
import { UserCog, Plus, PencilLine, Trash2 } from "lucide-react";
import {
  getAdminEmployees,
  deleteAdminEmployee,
  type AdminEmployee,
} from "@/lib/api/adminEmployees";
import ConfirmModal from "../ConfirmModal";
import { useAdminList } from "../useAdminList";

export default function AdminEmployeesPage() {
  const {
    items: employees,
    error,
    deleteTarget,
    setDeleteTarget,
    handleDelete,
  } = useAdminList<AdminEmployee>({
    fetchList: getAdminEmployees,
    deleteItem: deleteAdminEmployee,
    getId: (e) => e.id!,
    loadErrorMessage: "従業員一覧の取得に失敗しました",
    deleteSuccessMessage: (e) => `「${e.email}」を削除しました`,
  });

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (employees === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <div className="page-heading">
        <h1>
          <UserCog size={22} />
          従業員管理
        </h1>
        <span className="page-count">全 {employees.length} 件</span>
      </div>
      <Link href="/admin/employees/new" className="btn-primary">
        <Plus size={16} />
        新規従業員
      </Link>
      <ul className="admin-list compact">
        {employees.map((employee) => (
          <li key={employee.id}>
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <span className="card-icon">
                <UserCog size={18} />
              </span>
              <Link href={`/admin/employees/${employee.id}`}>
                {employee.email}
              </Link>
              <span className="card-tag">{employee.role}</span>
            </div>
            <span className="item-actions">
              <Link
                href={`/admin/employees/${employee.id}`}
                className="icon-btn"
                title="編集"
              >
                <PencilLine size={16} />
              </Link>
              <button
                className="icon-btn icon-btn-danger"
                title="削除"
                onClick={() => setDeleteTarget(employee)}
              >
                <Trash2 size={16} />
              </button>
            </span>
          </li>
        ))}
      </ul>

      <ConfirmModal
        open={deleteTarget !== null}
        title="従業員を削除しますか？"
        message={`「${deleteTarget?.email}」を削除します。この操作は取り消せません。`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </main>
  );
}
