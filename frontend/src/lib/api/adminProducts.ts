import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type AdminProduct = components["schemas"]["AdminProductResponse"];
export type CreateProductRequest = components["schemas"]["CreateProductRequest"];
export type UpdateProductRequest = components["schemas"]["UpdateProductRequest"];

export function getAdminProducts(): Promise<AdminProduct[]> {
  return apiFetch<AdminProduct[]>("/api/admin/products");
}

export function getAdminProduct(id: number): Promise<AdminProduct> {
  return apiFetch<AdminProduct>(`/api/admin/products/${id}`);
}

export function createAdminProduct(req: CreateProductRequest): Promise<AdminProduct> {
  return apiFetch<AdminProduct>("/api/admin/products", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function updateAdminProduct(id: number, req: UpdateProductRequest): Promise<AdminProduct> {
  return apiFetch<AdminProduct>(`/api/admin/products/${id}`, {
    method: "PUT",
    body: JSON.stringify(req),
  });
}

export function deleteAdminProduct(id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/products/${id}`, { method: "DELETE" });
}
