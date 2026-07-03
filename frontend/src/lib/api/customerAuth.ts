import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type CustomerLoginRequest = components["schemas"]["CustomerLoginRequest"];

export function customerLogin(req: CustomerLoginRequest): Promise<{ message: string }> {
  return apiFetch<{ message: string }>("/api/customer/auth/login", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function customerLogout(): Promise<void> {
  return apiFetch<void>("/api/customer/auth/logout", { method: "POST" });
}
