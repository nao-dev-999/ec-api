"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  getRecommendedProducts,
  type RecommendedProduct,
} from "@/lib/api/recommendations";

export default function RecommendedProducts({
  productId,
}: {
  productId: number;
}) {
  const [items, setItems] = useState<RecommendedProduct[]>([]);

  useEffect(() => {
    let cancelled = false;
    getRecommendedProducts(productId)
      .then((result) => {
        if (!cancelled) setItems(result);
      })
      .catch(() => {
        if (!cancelled) setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, [productId]);

  if (items.length === 0) return null;

  return (
    <section style={{ marginTop: 24 }}>
      <h2>この商品を見た人はこんな商品も見ています</h2>
      <ul>
        {items.map((item) => (
          <li key={item.productId}>
            <Link href={`/products/${item.productId}`}>{item.name}</Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
