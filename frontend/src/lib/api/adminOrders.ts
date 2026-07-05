import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type AdminOrder = components["schemas"]["AdminOrderResponse"];
export type OrderStatus = NonNullable<AdminOrder["status"]>;
export type AdminOrderPage =
  components["schemas"]["PageResponseAdminOrderResponse"];

export function getAdminOrders(page = 0, size = 20): Promise<AdminOrderPage> {
  return apiFetch<AdminOrderPage>(
    `/api/admin/orders?page=${page}&size=${size}`,
  );
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
