import type { ApiErrorBody } from "@/types/api";

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

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
  return `Yêu cầu thất bại (${status})`;
}

/** Lỗi hiển thị cho người dùng, kể cả khi không kết nối được máy chủ. */
export function errorMessage(e: unknown): string {
  if (e instanceof ApiRequestError) return e.message;
  // Lỗi kiểm tra phía giao diện ném Error thường, lỗi mạng của fetch là TypeError
  if (e instanceof Error && !(e instanceof TypeError)) return e.message;
  return "Không kết nối được tới máy chủ. Kiểm tra mạng rồi thử lại.";
}

type Query = Record<string, string | number | boolean | null | undefined>;

export interface ApiOptions extends Omit<RequestInit, "body"> {
  token?: string | null;
  /** Object được gửi dạng JSON, FormData gửi dạng multipart. */
  body?: unknown;
  query?: Query;
}

/** Lắng nghe lỗi 401 để AuthProvider tự đăng xuất. */
type UnauthorizedListener = () => void;
let onUnauthorized: UnauthorizedListener | null = null;
export function setUnauthorizedListener(listener: UnauthorizedListener | null): void {
  onUnauthorized = listener;
}

export function buildUrl(path: string, query?: Query): string {
  const url = `${API_BASE_URL}${path}`;
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

/** Gọi API Spring, tự gắn JWT và chuyển lỗi ApiError thành ApiRequestError. */
export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const response = await request(path, options);
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

/** Tải tệp cần JWT (ảnh, phiếu khám) để hiển thị qua object URL. */
export async function apiBlob(path: string, token: string | null): Promise<Blob> {
  const response = await request(path, { token });
  return response.blob();
}

export function publicPhotoUrl(petId: number, photoId: number): string {
  return `${API_BASE_URL}/api/public/pets/${petId}/photos/${photoId}`;
}
