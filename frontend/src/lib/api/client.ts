const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public code?: string,
    public details?: Record<string, string> | null,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

function redirectToLogin(path: string) {
  if (typeof window === "undefined" || path.endsWith("/login")) return;

  const isAdminPath =
    path.startsWith("/api/admin") || path.startsWith("/api/auth");
  const loginPath = isAdminPath ? "/admin/login" : "/login";

  if (window.location.pathname !== loginPath) {
    window.location.href = loginPath;
  }
}

export async function apiFetch<T>(
  path: string,
  init?: RequestInit & { suppressAuthRedirect?: boolean },
): Promise<T> {
  const { suppressAuthRedirect, ...fetchInit } = init ?? {};
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...fetchInit,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...fetchInit.headers,
    },
  });

  if (res.status === 401 && !suppressAuthRedirect) {
    redirectToLogin(path);
  }

  if (!res.ok) {
    const raw = await res.text().catch(() => "");
    let message = raw || res.statusText;
    let code: string | undefined;
    let details: Record<string, string> | undefined;

    if (raw) {
      try {
        const parsed = JSON.parse(raw) as {
          code?: string;
          message?: string;
          details?: Record<string, string> | null;
        };
        if (typeof parsed.message === "string" && parsed.message) {
          message = parsed.message;
        }
        code = parsed.code;
        details = parsed.details ?? undefined;
      } catch {
        // 非JSON応答はそのままテキストをmessageとして扱う
      }
    }

    throw new ApiError(res.status, message, code, details);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return res.json() as Promise<T>;
}
