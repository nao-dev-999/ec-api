"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Heart } from "lucide-react";
import { addWishlistItem, removeWishlistItem } from "@/lib/api/wishlist";
import { ApiError } from "@/lib/api/client";
import { getErrorMessage } from "@/lib/errors/messages";

export default function WishlistButton({ productId }: { productId: number }) {
  const router = useRouter();
  const [added, setAdded] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

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
      <button onClick={handleToggle} disabled={submitting}>
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
