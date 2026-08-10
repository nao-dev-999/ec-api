import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

/**
 * NOTE: imageUrl はバックエンドに追加済みだが、schema.d.ts はまだ再生成前のため
 * 交差型で補っている。`npm run generate:api-types` 実行後は生成された型に
 * 直接含まれるはずなので、この交差型は不要になる（形は同一）。
 */
export type AdminProduct = components["schemas"]["AdminProductResponse"] & {
  imageUrl?: string | null;
};
export type CreateProductRequest =
  components["schemas"]["CreateProductRequest"] & {
    imageUrl?: string | null;
  };
export type UpdateProductRequest =
  components["schemas"]["UpdateProductRequest"] & {
    imageUrl?: string | null;
  };

export function getAdminProducts(): Promise<AdminProduct[]> {
  return apiFetch<AdminProduct[]>("/api/admin/products");
}

export function getAdminProduct(id: number): Promise<AdminProduct> {
  return apiFetch<AdminProduct>(`/api/admin/products/${id}`);
}

export function createAdminProduct(
  req: CreateProductRequest,
): Promise<AdminProduct> {
  return apiFetch<AdminProduct>("/api/admin/products", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function updateAdminProduct(
  id: number,
  req: UpdateProductRequest,
): Promise<AdminProduct> {
  return apiFetch<AdminProduct>(`/api/admin/products/${id}`, {
    method: "PUT",
    body: JSON.stringify(req),
  });
}

export function deleteAdminProduct(id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/products/${id}`, { method: "DELETE" });
}

export function getLowStockProducts(threshold = 10): Promise<AdminProduct[]> {
  return apiFetch<AdminProduct[]>(
    `/api/admin/products/low-stock?threshold=${threshold}`,
  );
}
