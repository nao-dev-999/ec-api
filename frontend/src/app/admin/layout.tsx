"use client";

import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { adminLogout } from "@/lib/api/auth";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();

  async function handleLogout() {
    await adminLogout();
    router.push("/admin/login");
  }

  if (pathname === "/admin/login") return <>{children}</>;

  return (
    <div>
      <nav className="site-nav">
        <Link href="/admin">管理トップ</Link>
        <Link href="/admin/products">商品管理</Link>
        <Link href="/admin/categories">カテゴリ管理</Link>
        <Link href="/admin/customers">顧客管理</Link>
        <Link href="/admin/employees">従業員管理</Link>
        <Link href="/admin/orders">注文管理</Link>
        <button onClick={handleLogout}>ログアウト</button>
      </nav>
      {children}
    </div>
  );
}
