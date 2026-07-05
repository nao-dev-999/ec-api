import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type CartItem = components["schemas"]["CartItemResponse"];
export type AddCartItemRequest = components["schemas"]["AddCartItemRequest"];
export type UpdateCartItemQuantityRequest =
  components["schemas"]["UpdateCartItemQuantityRequest"];

export function getCart(): Promise<CartItem[]> {
  return apiFetch<CartItem[]>("/api/customer/cart");
}

export function addCartItem(req: AddCartItemRequest): Promise<CartItem> {
  return apiFetch<CartItem>("/api/customer/cart/items", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function updateCartItemQuantity(
  productId: number,
  req: UpdateCartItemQuantityRequest,
): Promise<CartItem> {
  return apiFetch<CartItem>(`/api/customer/cart/items/${productId}`, {
    method: "PATCH",
    body: JSON.stringify(req),
  });
}

export function removeCartItem(productId: number): Promise<void> {
  return apiFetch<void>(`/api/customer/cart/items/${productId}`, {
    method: "DELETE",
  });
}

export function clearCart(): Promise<void> {
  return apiFetch<void>("/api/customer/cart", { method: "DELETE" });
}
