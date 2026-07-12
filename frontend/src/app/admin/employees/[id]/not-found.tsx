import Link from "next/link";
import { ArrowLeft } from "lucide-react";

export default function NotFound() {
  return (
    <main>
      <Link href="/admin/employees" className="back-link">
        <ArrowLeft size={14} />
        従業員一覧に戻る
      </Link>
      <h1>従業員が見つかりません</h1>
      <p>指定された従業員は存在しないか、削除された可能性があります。</p>
    </main>
  );
}
