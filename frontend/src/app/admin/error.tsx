"use client";

import { useEffect } from "react";

export default function AdminError({
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
        管理画面の表示中に問題が発生しました。しばらくしてから再度お試しください。
      </p>
      <button onClick={reset}>再読み込み</button>
    </main>
  );
}
