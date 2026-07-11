import { ApiError } from "@/lib/api/client";

const ERROR_MESSAGES: Record<string, string> = {
  AUTHENTICATION_FAILED: "メールアドレスまたはパスワードが正しくありません",
  INVALID_CURRENT_PASSWORD: "現在のパスワードが正しくありません",
  OPTIMISTIC_LOCK_CONFLICT:
    "他の変更と競合しました。画面を更新して再度お試しください",
};

export function getErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof ApiError && err.code) {
    const known = ERROR_MESSAGES[err.code];
    if (known) return known;
  }
  return fallback;
}
