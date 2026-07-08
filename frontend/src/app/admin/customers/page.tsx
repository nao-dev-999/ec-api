"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Users, Eye, Trash2 } from "lucide-react";
import {
  getAdminCustomers,
  deleteAdminCustomer,
  type AdminCustomer,
} from "@/lib/api/adminCustomers";
import ConfirmModal from "../ConfirmModal";
import { useToast } from "@/app/Toast";

export default function AdminCustomersPage() {
  const { showToast } = useToast();
  const [customers, setCustomers] = useState<AdminCustomer[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminCustomer | null>(null);

  useEffect(() => {
    getAdminCustomers()
      .then(setCustomers)
      .catch(() => setError("顧客一覧の取得に失敗しました"));
  }, []);

  async function handleDelete() {
    if (!deleteTarget) return;
    try {
      await deleteAdminCustomer(deleteTarget.id!);
      setCustomers((prev) => prev!.filter((c) => c.id !== deleteTarget.id));
      showToast(`「${deleteTarget.email}」を削除しました`);
    } catch {
      showToast("削除に失敗しました", "error");
    } finally {
      setDeleteTarget(null);
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (customers === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <div className="page-heading">
        <h1>
          <Users size={22} />
          顧客管理
        </h1>
        <span className="page-count">全 {customers.length} 件</span>
      </div>
      <ul className="admin-list compact">
        {customers.map((customer) => (
          <li key={customer.id}>
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <span className="card-icon">
                <Users size={18} />
              </span>
              <Link href={`/admin/customers/${customer.id}`}>
                {customer.email}
              </Link>
            </div>
            <span className="item-actions">
              <Link
                href={`/admin/customers/${customer.id}`}
                className="icon-btn"
                title="詳細"
              >
                <Eye size={16} />
              </Link>
              <button
                className="icon-btn icon-btn-danger"
                title="削除"
                onClick={() => setDeleteTarget(customer)}
              >
                <Trash2 size={16} />
              </button>
            </span>
          </li>
        ))}
      </ul>

      <ConfirmModal
        open={deleteTarget !== null}
        title="顧客を削除しますか？"
        message={`「${deleteTarget?.email}」を削除します。この操作は取り消せません。`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </main>
  );
}
