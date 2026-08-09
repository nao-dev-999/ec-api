import { ApiError } from "@/lib/api/client";

const ERROR_MESSAGES: Record<string, string> = {
  AUTHENTICATION_FAILED: "メールアドレスまたはパスワードが正しくありません",
  INVALID_CURRENT_PASSWORD: "現在のパスワードが正しくありません",
  OPTIMISTIC_LOCK_CONFLICT:
    "他の変更と競合しました。画面を更新して再度お試しください",
  REVIEW_NOT_ALLOWED:
    "配送完了した購入実績がある商品のみレビューを投稿できます",
  REVIEW_ALREADY_EXISTS: "この商品にはすでにレビューを投稿済みです",
};

export function getErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof ApiError && err.code) {
    const known = ERROR_MESSAGES[err.code];
    if (known) return known;
  }
  return fallback;
}
