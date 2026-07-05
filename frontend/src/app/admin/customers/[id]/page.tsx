"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { getAdminCustomer, type AdminCustomer } from "@/lib/api/adminCustomers";

export default function AdminCustomerDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [customer, setCustomer] = useState<AdminCustomer | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminCustomer(Number(id))
      .then(setCustomer)
      .catch(() => setError("顧客情報の取得に失敗しました"));
  }, [id]);

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (!customer) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <Link href="/admin/customers" className="back-link">
        <ArrowLeft size={14} />
        顧客一覧に戻る
      </Link>
      <h1>顧客詳細</h1>
      <p>ID: {customer.id}</p>
      <p>メールアドレス: {customer.email}</p>
      <p>登録日: {customer.createdAt}</p>
    </main>
  );
}
