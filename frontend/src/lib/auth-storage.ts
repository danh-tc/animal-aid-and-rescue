import type { AuthResponse } from "@/types/api";

const KEY = "aar.auth";

// localStorage có thể bị chặn (chế độ riêng tư), nên mọi thao tác đều bọc try/catch.
export function saveAuth(auth: AuthResponse): void {
  try {
    window.localStorage.setItem(KEY, JSON.stringify(auth));
  } catch {
    // bỏ qua
  }
}

export function loadAuth(): AuthResponse | null {
  try {
    const raw = window.localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  } catch {
    return null;
  }
}

export function clearAuth(): void {
  try {
    window.localStorage.removeItem(KEY);
  } catch {
    // bỏ qua
  }
}
