import { apiFetch } from "./client";

/**
 * NOTE: バックエンドを起動して `npm run generate:api-types` を実行すると
 * schema.d.ts に AdminCouponResponse 等の型が生成されるので、生成後は
 * `components["schemas"]["AdminCouponResponse"]` 等に置き換えるとよい（形は同一）。
 */
export type AdminCoupon = {
  id: number;
  code: string;
  discountAmount: number;
  validFrom: string | null;
  validTo: string | null;
  usageLimit: number | null;
  usageCount: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type AdminCouponPage = {
  content: AdminCoupon[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type CreateCouponRequest = {
  code: string;
  discountAmount: number;
  validFrom?: string | null;
  validTo?: string | null;
  usageLimit?: number | null;
};

export type UpdateCouponRequest = {
  discountAmount?: number | null;
  validFrom?: string | null;
  validTo?: string | null;
  usageLimit?: number | null;
  active?: boolean | null;
  version: number;
};

export function getAdminCoupons(page = 0, size = 20): Promise<AdminCouponPage> {
  return apiFetch<AdminCouponPage>(
    `/api/admin/coupons?page=${page}&size=${size}`,
  );
}

export function getAdminCoupon(id: number): Promise<AdminCoupon> {
  return apiFetch<AdminCoupon>(`/api/admin/coupons/${id}`);
}

export function createAdminCoupon(
  req: CreateCouponRequest,
): Promise<AdminCoupon> {
  return apiFetch<AdminCoupon>("/api/admin/coupons", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function updateAdminCoupon(
  id: number,
  req: UpdateCouponRequest,
): Promise<AdminCoupon> {
  return apiFetch<AdminCoupon>(`/api/admin/coupons/${id}`, {
    method: "PUT",
    body: JSON.stringify(req),
  });
}

export function deleteAdminCoupon(id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/coupons/${id}`, { method: "DELETE" });
}
