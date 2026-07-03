import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type Order = components["schemas"]["OrderResponse"];
export type OrderRequest = components["schemas"]["OrderRequest"];

export function createOrder(req: OrderRequest): Promise<Order> {
  return apiFetch<Order>("/api/orders", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function getOrder(id: number): Promise<Order> {
  return apiFetch<Order>(`/api/orders/${id}`);
}

export function getMyOrders(): Promise<Order[]> {
  return apiFetch<Order[]>("/api/orders");
}
