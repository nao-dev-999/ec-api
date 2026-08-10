import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

/**
 * NOTE: imageUrl はバックエンドに追加済みだが、schema.d.ts はまだ再生成前のため
 * 交差型で補っている。`npm run generate:api-types` 実行後は生成された型に
 * 直接含まれるはずなので、この交差型は不要になる（形は同一）。
 */
export type Product = components["schemas"]["ProductResponse"] & {
  imageUrl?: string | null;
};

export type ProductSearchParams = {
  name?: string;
  description?: string;
  price?: number;
};

export function getProducts(params?: ProductSearchParams): Promise<Product[]> {
  const query = new URLSearchParams();
  if (params?.name) query.set("name", params.name);
  if (params?.description) query.set("description", params.description);
  if (params?.price !== undefined) query.set("price", String(params.price));

  const qs = query.toString();
  return apiFetch<Product[]>(`/api/customer/products${qs ? `?${qs}` : ""}`);
}

export function getProduct(id: number): Promise<Product> {
  return apiFetch<Product>(`/api/customer/products/${id}`);
}
