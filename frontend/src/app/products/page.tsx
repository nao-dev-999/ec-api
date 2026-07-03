import Link from "next/link";
import { getProducts } from "@/lib/api/products";

export const dynamic = "force-dynamic";

export default async function ProductsPage() {
  const products = await getProducts();

  return (
    <main style={{ padding: 24 }}>
      <h1>商品一覧</h1>
      <ul>
        {products.map((product) => (
          <li key={product.id}>
            <Link href={`/products/${product.id}`}>
              {product.name} — ¥{product.price} (在庫: {product.stock})
            </Link>
          </li>
        ))}
      </ul>
    </main>
  );
}
