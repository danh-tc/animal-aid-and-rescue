import type { ApiErrorBody } from "@/types/api";

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ApiRequestError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: ApiErrorBody | null,
  ) {
    super(body?.message ?? `Yêu cầu thất bại (${status})`);
  }
}

interface ApiOptions extends Omit<RequestInit, "body"> {
  token?: string | null;
  body?: unknown;
}

/** Gọi API Spring, tự gắn JWT và chuyển lỗi ApiError thành ApiRequestError. */
export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const { token, body, headers, ...rest } = options;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: {
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    let errorBody: ApiErrorBody | null = null;
    try {
      errorBody = (await response.json()) as ApiErrorBody;
    } catch {
      errorBody = null;
    }
    throw new ApiRequestError(response.status, errorBody);
  }
  return (await response.json()) as T;
}
