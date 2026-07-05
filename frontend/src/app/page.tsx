import Link from "next/link";

export default function Home() {
  return (
    <main>
      <div className="hero">
        <h1>テックプラザ</h1>
        <p className="catch">毎日安い！家電もガジェットもまとめてお得に</p>
      </div>
      <ul>
        <li>
          <Link href="/login" style={{ flex: 1 }}>
            ユーザーログイン
          </Link>
        </li>
        <li>
          <Link href="/admin/login" style={{ flex: 1 }}>
            管理者ログイン
          </Link>
        </li>
      </ul>
    </main>
  );
}
