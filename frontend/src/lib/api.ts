import type { ApiErrorBody } from "@/types/api";

/** Public API URL embedded in the frontend at build time. */
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/**
 * Server-side rendering uses the Compose service name inside Docker. Browsers
 * use the public URL because they cannot resolve the internal `backend` host.
 */
function requestBaseUrl(): string {
  if (typeof window === "undefined") {
    return process.env.API_INTERNAL_BASE_URL ?? API_BASE_URL;
  }
  return API_BASE_URL;
}

export class ApiRequestError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: ApiErrorBody | null,
  ) {
    super(body?.message ?? messageForStatus(status));
  }

  get code(): string | undefined {
    return this.body?.code;
  }

  get fieldErrors(): Record<string, string> {
    return this.body?.fieldErrors ?? {};
  }
}

function messageForStatus(status: number): string {
  if (status === 401) return "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại";
  if (status === 403) return "Bạn không có quyền thực hiện thao tác này";
  if (status === 404) return "Không tìm thấy dữ liệu";
  if (status >= 500) return "Máy chủ đang gặp sự cố, vui lòng thử lại sau";
  return `Không thực hiện được yêu cầu (mã lỗi ${status})`;
}

/** Returns a user-facing message for API, validation, and network failures. */
export function errorMessage(e: unknown): string {
  if (e instanceof ApiRequestError) return e.message;
  if (e instanceof Error && !(e instanceof TypeError)) return e.message;
  return "Không kết nối được tới máy chủ. Kiểm tra mạng rồi thử lại.";
}

type Query = Record<string, string | number | boolean | null | undefined>;

export interface ApiOptions extends Omit<RequestInit, "body"> {
  token?: string | null;
  /** Plain objects are sent as JSON; FormData is sent as multipart data. */
  body?: unknown;
  query?: Query;
}

type UnauthorizedListener = () => void;
let onUnauthorized: UnauthorizedListener | null = null;
export function setUnauthorizedListener(listener: UnauthorizedListener | null): void {
  onUnauthorized = listener;
}

export function buildUrl(path: string, query?: Query): string {
  const url = `${requestBaseUrl()}${path}`;
  if (!query) return url;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== "") params.set(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

async function request(path: string, options: ApiOptions): Promise<Response> {
  const { token, body, headers, query, ...rest } = options;
  const isForm = typeof FormData !== "undefined" && body instanceof FormData;
  const response = await fetch(buildUrl(path, query), {
    ...rest,
    headers: {
      ...(body !== undefined && !isForm ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body === undefined ? undefined : isForm ? (body as FormData) : JSON.stringify(body),
  });

  if (!response.ok) {
    let errorBody: ApiErrorBody | null = null;
    try {
      errorBody = (await response.json()) as ApiErrorBody;
    } catch {
      errorBody = null;
    }
    if (response.status === 401 && token && onUnauthorized) onUnauthorized();
    throw new ApiRequestError(response.status, errorBody);
  }
  return response;
}

export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const response = await request(path, options);
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export async function apiBlob(path: string, token: string | null): Promise<Blob> {
  const response = await request(path, { token });
  return response.blob();
}

export function publicPhotoUrl(petId: number, photoId: number): string {
  return `${API_BASE_URL}/api/public/pets/${petId}/photos/${photoId}`;
}
