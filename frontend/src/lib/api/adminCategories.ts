import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type AdminCategory = components["schemas"]["AdminCategoryResponse"];
export type CreateCategoryRequest = components["schemas"]["CreateCategoryRequest"];
export type UpdateCategoryRequest = components["schemas"]["UpdateCategoryRequest"];

export function getAdminCategories(): Promise<AdminCategory[]> {
  return apiFetch<AdminCategory[]>("/api/admin/categories");
}

export function getAdminCategory(id: number): Promise<AdminCategory> {
  return apiFetch<AdminCategory>(`/api/admin/categories/${id}`);
}

export function createAdminCategory(req: CreateCategoryRequest): Promise<AdminCategory> {
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

export function getProductCategories(productId: number): Promise<AdminCategory[]> {
  return apiFetch<AdminCategory[]>(`/api/admin/products/${productId}/categories`);
}

export function addCategoryToProduct(productId: number, categoryId: number): Promise<void> {
  return apiFetch<void>(`/api/admin/products/${productId}/categories/${categoryId}`, {
    method: "POST",
  });
}

export function removeCategoryFromProduct(productId: number, categoryId: number): Promise<void> {
  return apiFetch<void>(`/api/admin/products/${productId}/categories/${categoryId}`, {
    method: "DELETE",
  });
}
