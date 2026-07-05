import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type Product = components["schemas"]["ProductResponse"];

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
