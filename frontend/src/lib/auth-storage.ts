import type { AuthResponse, Role } from "@/types/api";

const KEY = "aar.auth";
/** Cookie chỉ chứa vai trò để proxy chuyển hướng sớm. Quyền thật do backend kiểm tra qua JWT. */
export const ROLE_COOKIE = "aar_role";

interface StoredAuth extends AuthResponse {
  expiresAt: number;
}

// localStorage có thể bị chặn (chế độ riêng tư), nên mọi thao tác đều bọc try/catch.
export function saveAuth(auth: AuthResponse): AuthResponse {
  const expiresAt = Date.now() + auth.expiresInMinutes * 60_000;
  try {
    window.localStorage.setItem(KEY, JSON.stringify({ ...auth, expiresAt } satisfies StoredAuth));
  } catch {
    // bỏ qua
  }
  setRoleCookie(auth.user.role, auth.expiresInMinutes * 60);
  return auth;
}

export function loadAuth(): AuthResponse | null {
  try {
    const raw = window.localStorage.getItem(KEY);
    if (!raw) return null;
    const stored = JSON.parse(raw) as Partial<StoredAuth>;
    if (!stored.accessToken || !stored.user) return null;
    if (stored.expiresAt && stored.expiresAt < Date.now()) {
      clearAuth();
      return null;
    }
    return stored as AuthResponse;
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
  setRoleCookie(null, 0);
}

function setRoleCookie(role: Role | null, maxAgeSeconds: number): void {
  try {
    document.cookie = role
      ? `${ROLE_COOKIE}=${role}; Path=/; Max-Age=${maxAgeSeconds}; SameSite=Lax`
      : `${ROLE_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`;
  } catch {
    // bỏ qua
  }
}
