import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type CustomerMe = components["schemas"]["CustomerMeResponse"];
export type UpdateEmailRequest = components["schemas"]["UpdateEmailRequest"];
export type UpdatePasswordRequest =
  components["schemas"]["UpdatePasswordRequest"];

export function getMe(options?: {
  suppressAuthRedirect?: boolean;
}): Promise<CustomerMe> {
  return apiFetch<CustomerMe>("/api/customer/me", options);
}

export function updateEmail(req: UpdateEmailRequest): Promise<CustomerMe> {
  return apiFetch<CustomerMe>("/api/customer/me/email", {
    method: "PATCH",
    body: JSON.stringify(req),
  });
}

export function updatePassword(req: UpdatePasswordRequest): Promise<void> {
  return apiFetch<void>("/api/customer/me/password", {
    method: "PATCH",
    body: JSON.stringify(req),
  });
}
