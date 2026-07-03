import Link from "next/link";

export default function AdminPage() {
  return (
    <main style={{ padding: 24 }}>
      <h1>管理画面</h1>
      <ul>
        <li>
          <Link href="/admin/products">商品管理</Link>
        </li>
        <li>
          <Link href="/admin/categories">カテゴリ管理</Link>
        </li>
        <li>
          <Link href="/admin/customers">顧客管理</Link>
        </li>
        <li>
          <Link href="/admin/employees">従業員管理</Link>
        </li>
        <li>
          <Link href="/admin/orders">注文管理</Link>
        </li>
      </ul>
    </main>
  );
}
