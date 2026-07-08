"use client";

import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { adminLogout } from "@/lib/api/auth";
import {
  ShoppingCart,
  LogOut,
  LayoutDashboard,
  Package,
  FolderTree,
  Users,
  UserCog,
  Receipt,
} from "lucide-react";
import "./admin.css";

const NAV_ITEMS = [
  { href: "/admin", label: "管理トップ", Icon: LayoutDashboard },
  { href: "/admin/products", label: "商品管理", Icon: Package },
  { href: "/admin/categories", label: "カテゴリ管理", Icon: FolderTree },
  { href: "/admin/customers", label: "顧客管理", Icon: Users },
  { href: "/admin/employees", label: "従業員管理", Icon: UserCog },
  { href: "/admin/orders", label: "注文管理", Icon: Receipt },
];

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const pathname = usePathname();

  async function handleLogout() {
    await adminLogout();
    router.push("/admin/login");
  }

  if (pathname === "/admin/login") {
    return (
      <div className="admin-shell admin-login-shell">
        <div className="admin-login-card">{children}</div>
      </div>
    );
  }

  return (
    <div className="admin-shell">
      <header className="admin-header">
        <Link href="/admin" className="admin-brand">
          <ShoppingCart size={20} />
          <span>テックプラザ管理</span>
        </Link>
        <div className="admin-header-right">
          <span className="admin-user">管理者</span>
          <button className="admin-logout" onClick={handleLogout}>
            <LogOut size={16} />
            <span>ログアウト</span>
          </button>
        </div>
      </header>
      <nav className="admin-nav">
        {NAV_ITEMS.map(({ href, label, Icon }) => (
          <Link
            key={href}
            href={href}
            className={"admin-nav-link" + (pathname === href ? " active" : "")}
          >
            <Icon size={16} />
            <span>{label}</span>
          </Link>
        ))}
      </nav>
      {children}
    </div>
  );
}
