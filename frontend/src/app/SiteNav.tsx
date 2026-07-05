"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import {
  Store,
  Package,
  ShoppingCart,
  Receipt,
  User,
  LogIn,
  LogOut,
} from "lucide-react";
import { getMe } from "@/lib/api/me";
import { customerLogout } from "@/lib/api/customerAuth";

const NAV_ITEMS = [
  { href: "/products", label: "商品一覧", Icon: Package },
  { href: "/cart", label: "カート", Icon: ShoppingCart },
  { href: "/orders", label: "注文履歴", Icon: Receipt },
  { href: "/mypage", label: "マイページ", Icon: User },
];

export default function SiteNav() {
  const pathname = usePathname();
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    getMe({ suppressAuthRedirect: true })
      .then(() => setIsLoggedIn(true))
      .catch(() => setIsLoggedIn(false));
  }, [pathname]);

  if (pathname.startsWith("/admin")) return null;

  async function handleLogout() {
    await customerLogout();
    setIsLoggedIn(false);
    router.push("/login");
  }

  return (
    <>
      <header className="site-header">
        <Link href="/products" className="brand">
          <Store size={20} />
          <span>テックプラザ</span>
        </Link>
      </header>
      <nav className="site-subnav">
        {NAV_ITEMS.map(({ href, label, Icon }) => (
          <Link
            key={href}
            href={href}
            className={pathname === href ? "active" : undefined}
          >
            <Icon size={16} />
            <span>{label}</span>
          </Link>
        ))}
        {isLoggedIn ? (
          <button type="button" onClick={handleLogout}>
            <LogOut size={16} />
            <span>ログアウト</span>
          </button>
        ) : (
          <Link
            href="/login"
            className={pathname === "/login" ? "active" : undefined}
          >
            <LogIn size={16} />
            <span>ログイン</span>
          </Link>
        )}
      </nav>
    </>
  );
}
