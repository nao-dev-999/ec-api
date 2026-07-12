import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type AdminCategory = components["schemas"]["AdminCategoryResponse"];
export type CreateCategoryRequest =
  components["schemas"]["CreateCategoryRequest"];
export type UpdateCategoryRequest =
  components["schemas"]["UpdateCategoryRequest"];

export function getAdminCategories(): Promise<AdminCategory[]> {
  return apiFetch<AdminCategory[]>("/api/admin/categories");
}

export function getAdminCategory(id: number): Promise<AdminCategory> {
  return apiFetch<AdminCategory>(`/api/admin/categories/${id}`);
}

export function createAdminCategory(
  req: CreateCategoryRequest,
): Promise<AdminCategory> {
  return apiFetch<AdminCategory>("/api/admin/categories", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function updateAdminCategory(
  id: number,
  req: UpdateCategoryRequest,
): Promise<AdminCategory> {
  return apiFetch<AdminCategory>(`/api/admin/categories/${id}`, {
    method: "PUT",
    body: JSON.stringify(req),
  });
}

export function deleteAdminCategory(id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/categories/${id}`, { method: "DELETE" });
}

// 商品一覧画面が全商品分のカテゴリをまとめて取得するため、同一セッション内での
// 再訪問・再レンダリングによる重複リクエストを避けるキャッシュ。
const productCategoriesCache = new Map<number, Promise<AdminCategory[]>>();

export function getProductCategories(
  productId: number,
): Promise<AdminCategory[]> {
  let cached = productCategoriesCache.get(productId);
  if (!cached) {
    cached = apiFetch<AdminCategory[]>(
      `/api/admin/products/${productId}/categories`,
    ).catch((err) => {
      productCategoriesCache.delete(productId);
      throw err;
    });
    productCategoriesCache.set(productId, cached);
  }
  return cached;
}

export function addCategoryToProduct(
  productId: number,
  categoryId: number,
): Promise<void> {
  productCategoriesCache.delete(productId);
  return apiFetch<void>(
    `/api/admin/products/${productId}/categories/${categoryId}`,
    {
      method: "POST",
    },
  );
}

export function removeCategoryFromProduct(
  productId: number,
  categoryId: number,
): Promise<void> {
  productCategoriesCache.delete(productId);
  return apiFetch<void>(
    `/api/admin/products/${productId}/categories/${categoryId}`,
    {
      method: "DELETE",
    },
  );
}
