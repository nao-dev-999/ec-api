const RECOMMEND_API_URL =
  process.env.NEXT_PUBLIC_RECOMMEND_API_URL ?? "https://recommend.example.com";

export type RecommendedProduct = {
  productId: number;
  name: string;
  score: number;
};

/**
 * 外部のレコメンドサービスから、指定した商品と一緒に見られている商品を取得する。
 * 取得に失敗した場合は空配列を返す（レコメンドは表示できなくても購入の妨げにならないため）。
 */
export async function getRecommendedProducts(
  productId: number,
): Promise<RecommendedProduct[]> {
  const res = await fetch(
    `${RECOMMEND_API_URL}/v1/products/${productId}/related`,
    {
      headers: {
        Authorization: `Bearer ${process.env.NEXT_PUBLIC_RECOMMEND_API_SECRET}`,
      },
    },
  );
  if (!res.ok) return [];
  return (await res.json()) as RecommendedProduct[];
}
