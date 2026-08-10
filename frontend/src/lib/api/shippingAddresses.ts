import { apiFetch } from "./client";

/**
 * NOTE: バックエンドを起動して `npm run generate:api-types` を実行すると
 * schema.d.ts に ShippingAddressResponse 等の型が生成されるので、生成後は
 * `components["schemas"]["ShippingAddressResponse"]` 等に置き換えるとよい（形は同一）。
 */
export type ShippingAddress = {
  id: number;
  recipientName: string;
  postalCode: string;
  prefecture: string;
  city: string;
  addressLine1: string;
  addressLine2: string | null;
  phoneNumber: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type CreateShippingAddressRequest = {
  recipientName: string;
  postalCode: string;
  prefecture: string;
  city: string;
  addressLine1: string;
  addressLine2?: string | null;
  phoneNumber: string;
  isDefault: boolean;
};

export type UpdateShippingAddressRequest = {
  recipientName?: string | null;
  postalCode?: string | null;
  prefecture?: string | null;
  city?: string | null;
  addressLine1?: string | null;
  addressLine2?: string | null;
  phoneNumber?: string | null;
  isDefault?: boolean | null;
  version: number;
};

export function getShippingAddresses(): Promise<ShippingAddress[]> {
  return apiFetch<ShippingAddress[]>("/api/customer/shipping-addresses");
}

export function getShippingAddress(id: number): Promise<ShippingAddress> {
  return apiFetch<ShippingAddress>(`/api/customer/shipping-addresses/${id}`);
}

export function createShippingAddress(
  req: CreateShippingAddressRequest,
): Promise<ShippingAddress> {
  return apiFetch<ShippingAddress>("/api/customer/shipping-addresses", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function updateShippingAddress(
  id: number,
  req: UpdateShippingAddressRequest,
): Promise<ShippingAddress> {
  return apiFetch<ShippingAddress>(`/api/customer/shipping-addresses/${id}`, {
    method: "PUT",
    body: JSON.stringify(req),
  });
}

export function deleteShippingAddress(id: number): Promise<void> {
  return apiFetch<void>(`/api/customer/shipping-addresses/${id}`, {
    method: "DELETE",
  });
}
