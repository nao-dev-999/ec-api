import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

/**
 * NOTE: couponCode/discountAmount はバックエンドに追加済みだが、
 * schema.d.ts はまだ再生成前のため交差型で補っている。
 * `npm run generate:api-types` 実行後は生成された型に直接含まれるはずなので、
 * この交差型は不要になる（形は同一）。
 */
export type Order = components["schemas"]["OrderResponse"] & {
  couponCode?: string | null;
  discountAmount?: number;
};
export type OrderRequest = components["schemas"]["OrderRequest"] & {
  couponCode?: string | null;
};
export type OrderPage = components["schemas"]["PageResponseOrderResponse"];

export function createOrder(req: OrderRequest): Promise<Order> {
  return apiFetch<Order>("/api/orders", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function getOrder(id: number): Promise<Order> {
  return apiFetch<Order>(`/api/orders/${id}`);
}

export function getMyOrders(page = 0, size = 20): Promise<OrderPage> {
  return apiFetch<OrderPage>(`/api/orders?page=${page}&size=${size}`);
}
