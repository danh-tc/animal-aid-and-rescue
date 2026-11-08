"use client";

import { useRouter } from "next/navigation";
import { createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { setUnauthorizedListener } from "@/lib/api";
import { clearAuth, loadAuth, saveAuth } from "@/lib/auth-storage";
import type { AuthResponse, Role, UserView } from "@/types/api";

interface AuthState {

  ready: boolean;
  user: UserView | null;
  token: string | null;
  signIn: (auth: AuthResponse) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [auth, setAuth] = useState<AuthResponse | null>(null);

  useEffect(() => {
    setAuth(loadAuth());
    setReady(true);
  }, []);

  const signIn = useCallback((value: AuthResponse) => {
    setAuth(saveAuth(value));
  }, []);

  const signOut = useCallback(() => {
    clearAuth();
    setAuth(null);
  }, []);

  useEffect(() => {
    setUnauthorizedListener(() => {
      clearAuth();
      setAuth(null);
      router.replace(`/login?next=${encodeURIComponent(window.location.pathname)}&expired=1`);
    });
    return () => setUnauthorizedListener(null);
  }, [router]);

  const value = useMemo<AuthState>(
    () => ({ ready, user: auth?.user ?? null, token: auth?.accessToken ?? null, signIn, signOut }),
    [ready, auth, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth phải nằm trong AuthProvider");
  return ctx;
}

export function isStaff(role: Role | undefined | null): boolean {
  return role === "STAFF" || role === "MANAGER";
}

export function homeFor(role: Role): string {
  if (role === "ADOPTER") return "/me/applications";
  return "/staff";
}

export function safeNext(next: string | null, role: Role): string {
  if (next && next.startsWith("/") && !next.startsWith("//")) {
    if (next.startsWith("/staff") && role === "ADOPTER") return homeFor(role);
    if ((next.startsWith("/me") || next.includes("/apply")) && role !== "ADOPTER") return homeFor(role);
    if (next.startsWith("/admin") && role !== "MANAGER") return homeFor(role);
    return next;
  }
  return homeFor(role);
}
