import Link from "next/link";
import { ArrowLeft } from "lucide-react";

export default function NotFound() {
  return (
    <main>
      <Link href="/admin/customers" className="back-link">
        <ArrowLeft size={14} />
        顧客一覧に戻る
      </Link>
      <h1>顧客が見つかりません</h1>
      <p>指定された顧客は存在しないか、削除された可能性があります。</p>
    </main>
  );
}
