import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { getProduct } from "@/lib/api/products";
import { ApiError } from "@/lib/api/client";
import AddToCartButton from "./AddToCartButton";

export const dynamic = "force-dynamic";

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  let product;
  try {
    product = await getProduct(Number(id));
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  return (
    <main style={{ maxWidth: 480 }}>
      <Link href="/products" className="back-link">
        <ArrowLeft size={14} />
        商品一覧に戻る
      </Link>
      <h1>{product.name}</h1>
      <p style={{ marginBottom: 12 }}>{product.description}</p>
      <p style={{ marginBottom: 8 }}>
        <span className="price price-lg">¥{product.price}</span>
      </p>
      <p style={{ marginBottom: 16 }}>
        <span className="badge">在庫: {product.stock}</span>
      </p>
      <AddToCartButton productId={product.id!} />
    </main>
  );
}
