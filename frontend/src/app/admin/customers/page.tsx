"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  getAdminCustomers,
  deleteAdminCustomer,
  type AdminCustomer,
} from "@/lib/api/adminCustomers";

export default function AdminCustomersPage() {
  const [customers, setCustomers] = useState<AdminCustomer[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminCustomers()
      .then(setCustomers)
      .catch(() => setError("顧客一覧の取得に失敗しました"));
  }, []);

  async function handleDelete(id: number) {
    if (!confirm("この顧客を削除しますか？")) return;
    try {
      await deleteAdminCustomer(id);
      setCustomers((prev) => prev!.filter((c) => c.id !== id));
    } catch {
      setError("削除に失敗しました");
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (customers === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main style={{ padding: 24 }}>
      <h1>顧客管理</h1>
      <ul>
        {customers.map((customer) => (
          <li key={customer.id} style={{ marginBottom: 8 }}>
            <Link href={`/admin/customers/${customer.id}`}>{customer.email}</Link>{" "}
            <button onClick={() => handleDelete(customer.id!)}>削除</button>
          </li>
        ))}
      </ul>
    </main>
  );
}
