import { getProduct } from "@/lib/api/products";
import AddToCartButton from "./AddToCartButton";

export const dynamic = "force-dynamic";

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const product = await getProduct(Number(id));

  return (
    <main style={{ padding: 24 }}>
      <h1>{product.name}</h1>
      <p>{product.description}</p>
      <p>価格: ¥{product.price}</p>
      <p>在庫: {product.stock}</p>
      <AddToCartButton productId={product.id!} />
    </main>
  );
}
