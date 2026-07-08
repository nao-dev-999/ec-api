import Link from "next/link";
import { ArrowLeft } from "lucide-react";

export default function NotFound() {
  return (
    <main style={{ maxWidth: 480 }}>
      <Link href="/products" className="back-link">
        <ArrowLeft size={14} />
        商品一覧に戻る
      </Link>
      <h1>商品が見つかりません</h1>
      <p>指定された商品は存在しないか、削除された可能性があります。</p>
    </main>
  );
}
