import Link from "next/link";
import {
  LayoutDashboard,
  Package,
  FolderTree,
  Users,
  UserCog,
  Receipt,
} from "lucide-react";

export default function AdminPage() {
  return (
    <main>
      <h1>
        <LayoutDashboard size={22} />
        管理画面
      </h1>
      <ul>
        <li>
          <Link href="/admin/products">
            <Package size={18} style={{ marginRight: 8 }} />
            商品管理
          </Link>
        </li>
        <li>
          <Link href="/admin/categories">
            <FolderTree size={18} style={{ marginRight: 8 }} />
            カテゴリ管理
          </Link>
        </li>
        <li>
          <Link href="/admin/customers">
            <Users size={18} style={{ marginRight: 8 }} />
            顧客管理
          </Link>
        </li>
        <li>
          <Link href="/admin/employees">
            <UserCog size={18} style={{ marginRight: 8 }} />
            従業員管理
          </Link>
        </li>
        <li>
          <Link href="/admin/orders">
            <Receipt size={18} style={{ marginRight: 8 }} />
            注文管理
          </Link>
        </li>
      </ul>
    </main>
  );
}
