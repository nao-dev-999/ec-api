import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type AdminOrder = components["schemas"]["AdminOrderResponse"];
export type OrderStatus = NonNullable<AdminOrder["status"]>;

export function getAdminOrders(): Promise<AdminOrder[]> {
  return apiFetch<AdminOrder[]>("/api/admin/orders");
}

export function getAdminOrder(id: number): Promise<AdminOrder> {
  return apiFetch<AdminOrder>(`/api/admin/orders/${id}`);
}

export function updateAdminOrderStatus(
  id: number,
  status: OrderStatus,
  version: number,
): Promise<AdminOrder> {
  return apiFetch<AdminOrder>(
    `/api/admin/orders/${id}/status?status=${status}&version=${version}`,
    { method: "PATCH" },
  );
}
