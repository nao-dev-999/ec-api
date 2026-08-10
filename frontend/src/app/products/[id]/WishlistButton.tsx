"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Heart } from "lucide-react";
import {
  addWishlistItem,
  getWishlistItem,
  removeWishlistItem,
} from "@/lib/api/wishlist";
import { ApiError } from "@/lib/api/client";
import { getErrorMessage } from "@/lib/errors/messages";

export default function WishlistButton({ productId }: { productId: number }) {
  const router = useRouter();
  const [added, setAdded] = useState(false);
  const [checking, setChecking] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getWishlistItem(productId, { suppressAuthRedirect: true })
      .then(() => {
        if (!cancelled) setAdded(true);
      })
      .catch(() => {
        // 404（未登録）・401（未ログイン）いずれも未登録として扱う（ゲスト閲覧を妨げない）
        if (!cancelled) setAdded(false);
      })
      .finally(() => {
        if (!cancelled) setChecking(false);
      });
    return () => {
      cancelled = true;
    };
  }, [productId]);

  async function handleToggle() {
    setMessage(null);
    setSubmitting(true);
    try {
      if (added) {
        await removeWishlistItem(productId);
        setAdded(false);
      } else {
        await addWishlistItem(productId);
        setAdded(true);
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.push("/login");
        return;
      }
      setMessage(getErrorMessage(err, "お気に入りの更新に失敗しました"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ marginTop: 12 }}>
      <button onClick={handleToggle} disabled={submitting || checking}>
        <Heart
          size={16}
          fill={added ? "currentColor" : "none"}
          style={{ marginRight: 6, verticalAlign: "text-bottom" }}
        />
        {added ? "お気に入り登録済み" : "お気に入りに追加"}
      </button>
      {message && <p>{message}</p>}
    </div>
  );
}
