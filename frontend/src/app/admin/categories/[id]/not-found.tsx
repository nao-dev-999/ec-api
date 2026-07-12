import Link from "next/link";
import { ArrowLeft } from "lucide-react";

export default function NotFound() {
  return (
    <main>
      <Link href="/admin/categories" className="back-link">
        <ArrowLeft size={14} />
        カテゴリ一覧に戻る
      </Link>
      <h1>カテゴリが見つかりません</h1>
      <p>指定されたカテゴリは存在しないか、削除された可能性があります。</p>
    </main>
  );
}
