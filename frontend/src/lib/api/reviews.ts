import { apiFetch } from "./client";

/**
 * NOTE: バックエンドを起動して `npm run generate:api-types` を実行すると
 * schema.d.ts に ReviewResponse 等の型が生成されるので、生成後は
 * `components["schemas"]["ReviewResponse"]` 等に置き換えるとよい（形は同一）。
 */
export type Review = {
  id: number;
  productId: number;
  customerId: number;
  customerName: string | null;
  rating: number;
  comment: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type ReviewSummary = {
  averageRating: number;
  reviewCount: number;
};

export type ReviewPage = {
  content: Review[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ProductReviews = {
  reviews: ReviewPage;
  summary: ReviewSummary;
};

export type CreateReviewRequest = {
  productId: number;
  rating: number;
  comment?: string;
};

export type UpdateReviewRequest = {
  rating: number;
  comment?: string;
  version: number;
};

export function getProductReviews(
  productId: number,
  page = 0,
  size = 20,
): Promise<ProductReviews> {
  return apiFetch<ProductReviews>(
    `/api/customer/products/${productId}/reviews?page=${page}&size=${size}`,
  );
}

export function createReview(req: CreateReviewRequest): Promise<Review> {
  return apiFetch<Review>("/api/customer/reviews", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function updateReview(
  id: number,
  req: UpdateReviewRequest,
): Promise<Review> {
  return apiFetch<Review>(`/api/customer/reviews/${id}`, {
    method: "PUT",
    body: JSON.stringify(req),
  });
}

export function deleteReview(id: number): Promise<void> {
  return apiFetch<void>(`/api/customer/reviews/${id}`, { method: "DELETE" });
}
