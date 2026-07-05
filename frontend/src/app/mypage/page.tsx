"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  getMe,
  updateEmail,
  updatePassword,
  type CustomerMe,
} from "@/lib/api/me";
import { ApiError } from "@/lib/api/client";

export default function MyPage() {
  const router = useRouter();
  const [me, setMe] = useState<CustomerMe | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [email, setEmail] = useState("");
  const [emailMessage, setEmailMessage] = useState<string | null>(null);
  const [emailSubmitting, setEmailSubmitting] = useState(false);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [passwordMessage, setPasswordMessage] = useState<string | null>(null);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);

  useEffect(() => {
    getMe()
      .then((result) => {
        setMe(result);
        setEmail(result.email ?? "");
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.push("/login");
          return;
        }
        setLoadError("会員情報の取得に失敗しました");
      });
  }, [router]);

  async function handleEmailSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!me) return;
    setEmailMessage(null);
    setEmailSubmitting(true);
    try {
      const updated = await updateEmail({ email, version: me.version! });
      setMe(updated);
      setEmailMessage("メールアドレスを更新しました");
    } catch (err) {
      setEmailMessage(
        err instanceof ApiError && err.status === 409
          ? "他の変更と競合しました。画面を更新して再度お試しください"
          : "メールアドレスの更新に失敗しました",
      );
    } finally {
      setEmailSubmitting(false);
    }
  }

  async function handlePasswordSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!me) return;
    setPasswordMessage(null);
    setPasswordSubmitting(true);
    try {
      await updatePassword({
        currentPassword,
        newPassword,
        version: me.version!,
      });
      const refreshed = await getMe();
      setMe(refreshed);
      setCurrentPassword("");
      setNewPassword("");
      setPasswordMessage("パスワードを更新しました");
    } catch (err) {
      setPasswordMessage(
        err instanceof ApiError && err.status === 400
          ? "現在のパスワードが正しくありません"
          : "パスワードの更新に失敗しました",
      );
    } finally {
      setPasswordSubmitting(false);
    }
  }

  if (loadError)
    return <p style={{ padding: 24, color: "red" }}>{loadError}</p>;
  if (!me) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main style={{ padding: 24, maxWidth: 400 }}>
      <h1>マイページ</h1>

      <section style={{ marginBottom: 32 }}>
        <h2>メールアドレス変更</h2>
        <form onSubmit={handleEmailSubmit}>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={{ display: "block", width: "100%", marginBottom: 8 }}
          />
          <button type="submit" disabled={emailSubmitting}>
            {emailSubmitting ? "更新中..." : "メールアドレスを更新"}
          </button>
          {emailMessage && <p>{emailMessage}</p>}
        </form>
      </section>

      <section>
        <h2>パスワード変更</h2>
        <form onSubmit={handlePasswordSubmit}>
          <div style={{ marginBottom: 8 }}>
            <label htmlFor="currentPassword">現在のパスワード</label>
            <input
              id="currentPassword"
              type="password"
              required
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <div style={{ marginBottom: 8 }}>
            <label htmlFor="newPassword">新しいパスワード（8文字以上）</label>
            <input
              id="newPassword"
              type="password"
              required
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              style={{ display: "block", width: "100%" }}
            />
          </div>
          <button type="submit" disabled={passwordSubmitting}>
            {passwordSubmitting ? "更新中..." : "パスワードを更新"}
          </button>
          {passwordMessage && <p>{passwordMessage}</p>}
        </form>
      </section>
    </main>
  );
}
