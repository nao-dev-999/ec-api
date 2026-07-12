import Link from "next/link";
import { ArrowLeft } from "lucide-react";

export default function NotFound() {
  return (
    <main>
      <Link href="/orders" className="back-link">
        <ArrowLeft size={14} />
        注文履歴に戻る
      </Link>
      <h1>注文が見つかりません</h1>
      <p>指定された注文は存在しないか、削除された可能性があります。</p>
    </main>
  );
}
