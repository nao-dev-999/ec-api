import { apiFetch } from "./client";

/**
 * NOTE: schema.d.ts 生成後は components["schemas"]["AdminReviewResponse"] /
 * "PageResponseAdminReviewResponse" に置き換え可能（形は同一）。
 */
export type AdminReview = {
  id: number;
  productId: number;
  productName: string | null;
  customerId: number;
  customerName: string | null;
  rating: number;
  comment: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type AdminReviewPage = {
  content: AdminReview[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export function getAdminReviews(page = 0, size = 20): Promise<AdminReviewPage> {
  return apiFetch<AdminReviewPage>(
    `/api/admin/reviews?page=${page}&size=${size}`,
  );
}

export function deleteAdminReview(id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/reviews/${id}`, { method: "DELETE" });
}
