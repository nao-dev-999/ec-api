import Link from "next/link";
import { getProducts } from "@/lib/api/products";

export const dynamic = "force-dynamic";

export default async function ProductsPage() {
  const products = await getProducts();

  return (
    <main>
      <h1>商品一覧</h1>
      <ul>
        {products.map((product) => (
          <li key={product.id}>
            <Link href={`/products/${product.id}`} style={{ flex: 1 }}>
              <span style={{ display: "block", fontWeight: 600 }}>
                {product.name}
              </span>
            </Link>
            <span className="badge">在庫: {product.stock}</span>
            <span className="price price-lg">¥{product.price}</span>
          </li>
        ))}
      </ul>
    </main>
  );
}
