import { apiFetch } from "./client";

/**
 * NOTE: バックエンドを起動して `npm run generate:api-types` を実行すると
 * schema.d.ts に WishlistItemResponse 等の型が生成されるので、生成後は
 * `components["schemas"]["WishlistItemResponse"]` 等に置き換えるとよい（形は同一）。
 */
export type WishlistItem = {
  id: number;
  productId: number;
  productName: string | null;
  price: number;
  stock: number;
  createdAt: string;
};

export type WishlistPage = {
  content: WishlistItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export function getWishlist(page = 0, size = 20): Promise<WishlistPage> {
  return apiFetch<WishlistPage>(
    `/api/customer/wishlist?page=${page}&size=${size}`,
  );
}

export function getWishlistItem(
  productId: number,
  options?: { suppressAuthRedirect?: boolean },
): Promise<WishlistItem> {
  return apiFetch<WishlistItem>(
    `/api/customer/wishlist/items/${productId}`,
    options,
  );
}

export function addWishlistItem(productId: number): Promise<WishlistItem> {
  return apiFetch<WishlistItem>("/api/customer/wishlist/items", {
    method: "POST",
    body: JSON.stringify({ productId }),
  });
}

export function removeWishlistItem(productId: number): Promise<void> {
  return apiFetch<void>(`/api/customer/wishlist/items/${productId}`, {
    method: "DELETE",
  });
}
