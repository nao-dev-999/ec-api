import { apiFetch } from "./client";
import type { components } from "./schema.d.ts";

export type AdminEmployee = components["schemas"]["AdminEmployeeResponse"];
export type CreateEmployeeRequest = components["schemas"]["CreateEmployeeRequest"];
export type UpdateEmployeeRoleRequest = components["schemas"]["UpdateEmployeeRoleRequest"];
export type EmployeeRole = CreateEmployeeRequest["role"];
export const EMPLOYEE_ROLES: EmployeeRole[] = ["ADMIN", "PRODUCT_MANAGER", "SALES"];

export function getAdminEmployees(): Promise<AdminEmployee[]> {
  return apiFetch<AdminEmployee[]>("/api/admin/employees");
}

export function getAdminEmployee(id: number): Promise<AdminEmployee> {
  return apiFetch<AdminEmployee>(`/api/admin/employees/${id}`);
}

export function createAdminEmployee(req: CreateEmployeeRequest): Promise<AdminEmployee> {
  return apiFetch<AdminEmployee>("/api/admin/employees", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export function updateAdminEmployeeRole(
  id: number,
  req: UpdateEmployeeRoleRequest,
): Promise<AdminEmployee> {
  return apiFetch<AdminEmployee>(`/api/admin/employees/${id}/role`, {
    method: "PATCH",
    body: JSON.stringify(req),
  });
}

export function deleteAdminEmployee(id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/employees/${id}`, { method: "DELETE" });
}
