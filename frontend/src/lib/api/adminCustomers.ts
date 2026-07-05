import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type AdminCustomer = components["schemas"]["AdminCustomerResponse"];

export function getAdminCustomers(): Promise<AdminCustomer[]> {
  return apiFetch<AdminCustomer[]>("/api/admin/customers");
}

export function getAdminCustomer(id: number): Promise<AdminCustomer> {
  return apiFetch<AdminCustomer>(`/api/admin/customers/${id}`);
}

export function deleteAdminCustomer(id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/customers/${id}`, { method: "DELETE" });
}
