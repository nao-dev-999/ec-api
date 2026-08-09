"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Heart, Trash2 } from "lucide-react";
import {
  getWishlist,
  removeWishlistItem,
  type WishlistItem,
} from "@/lib/api/wishlist";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

export default function WishlistPage() {
  const router = useRouter();
  const { showToast } = useToast();
  const [items, setItems] = useState<WishlistItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getWishlist()
      .then(setItems)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.push("/login");
          return;
        }
        setError(getErrorMessage(err, "お気に入りの取得に失敗しました"));
      });
  }, [router]);

  async function handleRemove(productId: number) {
    try {
      await removeWishlistItem(productId);
      setItems((prev) => prev!.filter((i) => i.productId !== productId));
    } catch (err) {
      showToast(getErrorMessage(err, "削除に失敗しました"), "error");
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (items === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <h1>
        <Heart
          size={20}
          style={{ marginRight: 6, verticalAlign: "text-bottom" }}
        />
        お気に入り
      </h1>
      {items.length === 0 ? (
        <p>お気に入りはまだありません</p>
      ) : (
        <ul>
          {items.map((item) => (
            <li key={item.id}>
              <span>
                <Link href={`/products/${item.productId}`}>
                  {item.productName}
                </Link>{" "}
                <span className="badge">¥{item.price}</span>
              </span>
              <span style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <span className="badge">在庫: {item.stock}</span>
                <button onClick={() => handleRemove(item.productId)}>
                  <Trash2 size={14} />
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
