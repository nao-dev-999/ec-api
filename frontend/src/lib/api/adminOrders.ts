import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

/**
 * NOTE: couponCode/discountAmount/配送先スナップショットはバックエンドに追加済みだが、
 * schema.d.ts はまだ再生成前のため交差型で補っている。
 * `npm run generate:api-types` 実行後は生成された型に直接含まれるはずなので、
 * この交差型は不要になる（形は同一）。
 */
export type AdminOrder = components["schemas"]["AdminOrderResponse"] & {
  couponCode?: string | null;
  discountAmount?: number;
  shippingRecipientName?: string | null;
  shippingPostalCode?: string | null;
  shippingPrefecture?: string | null;
  shippingCity?: string | null;
  shippingAddressLine1?: string | null;
  shippingAddressLine2?: string | null;
  shippingPhoneNumber?: string | null;
};
export type OrderStatus = NonNullable<AdminOrder["status"]>;
export type AdminOrderPage = Omit<
  components["schemas"]["PageResponseAdminOrderResponse"],
  "content"
> & {
  content?: AdminOrder[];
};

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
