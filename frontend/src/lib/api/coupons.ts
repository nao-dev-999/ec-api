import { apiFetch } from "./client";

/**
 * NOTE: バックエンドを起動して `npm run generate:api-types` を実行すると
 * schema.d.ts に CouponPreviewResponse の型が生成されるので、生成後は
 * `components["schemas"]["CouponPreviewResponse"]` に置き換えるとよい（形は同一）。
 */
export type CouponPreview = {
  code: string;
  discountAmount: number;
};

export function previewCoupon(
  code: string,
  subtotal: number,
): Promise<CouponPreview> {
  return apiFetch<CouponPreview>(
    `/api/customer/coupons/${encodeURIComponent(code)}/preview?subtotal=${subtotal}`,
  );
}
