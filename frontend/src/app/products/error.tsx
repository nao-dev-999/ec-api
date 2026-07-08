"use client";

import { useEffect } from "react";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main>
      <h1>エラーが発生しました</h1>
      <p style={{ color: "var(--danger)", marginBottom: 16 }}>
        商品情報の取得に失敗しました。しばらくしてから再度お試しください。
      </p>
      <button onClick={reset}>再読み込み</button>
    </main>
  );
}
