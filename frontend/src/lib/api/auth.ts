import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type LoginRequest = components["schemas"]["LoginRequest"];

export function adminLogin(req: LoginRequest): Promise<{ message: string }> {
  return apiFetch<{ message: string }>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function adminLogout(): Promise<void> {
  return apiFetch<void>("/api/auth/logout", { method: "POST" });
}
