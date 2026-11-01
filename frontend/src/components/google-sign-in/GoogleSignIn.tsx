"use client";

import { useEffect, useRef, useState } from "react";
import { AuthDivider } from "@/components/auth-shell/AuthShell";
import { useToast } from "@/components/toast/Toast";
import { apiFetch, errorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { AuthResponse } from "@/types/api";
import "./google-sign-in.scss";

interface GsiButtonOptions {
  type: "standard";
  theme: "outline" | "filled_blue";
  size: "large";
  text: "signin_with" | "signup_with" | "continue_with";
  shape: "rectangular";
  logo_alignment: "left";
  width: number;
  locale: string;
}

interface GoogleAccounts {
  accounts: {
    id: {
      initialize: (config: {
        client_id: string;
        callback: (r: { credential: string }) => void;
        ux_mode?: "popup";
        cancel_on_tap_outside?: boolean;
      }) => void;
      renderButton: (el: HTMLElement, options: GsiButtonOptions) => void;
    };
  };
}

declare global {
  interface Window {
    google?: GoogleAccounts;
  }
}

const SCRIPT_SRC = "https://accounts.google.com/gsi/client";
let scriptPromise: Promise<void> | null = null;

function loadScript(): Promise<void> {
  if (window.google?.accounts) return Promise.resolve();
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const el = document.createElement("script");
      el.src = SCRIPT_SRC;
      el.async = true;
      el.defer = true;
      el.onload = () => resolve();
      el.onerror = () => {
        scriptPromise = null;
        reject(new Error("Không tải được Google Sign-In"));
      };
      document.head.appendChild(el);
    });
  }
  return scriptPromise;
}

interface AuthConfig {
  googleEnabled: boolean;
  googleClientId: string | null;
}

interface GoogleAuthResponse extends AuthResponse {
  newAccount: boolean;
}

/**
 * Nút "Đăng nhập với Google" chính thức (Google Identity Services). Google trả ID token,
 * backend xác thực chữ ký rồi cấp JWT của hệ thống. Lần đầu dùng thì tạo tài khoản Người nhận nuôi.
 */
interface GoogleSignInProps {
  mode: "signin" | "signup";
  onError: (message: string) => void;
  /** Nhãn đường kẻ ngăn cách với form email, chỉ hiện khi nút Google hiện. */
  dividerLabel: string;
}

/** Ghi lý do không hiện nút Google ra console để nhóm phát triển kiểm tra, người dùng không thấy gì. */
function logHidden(reason: string, detail?: unknown) {
  console.warn(`[GoogleSignIn] Ẩn nút đăng nhập Google: ${reason}`, detail ?? "");
}

export function GoogleSignIn({ mode, onError, dividerLabel }: GoogleSignInProps) {
  const { signIn } = useAuth();
  const toast = useToast();
  const holder = useRef<HTMLDivElement>(null);
  const [state, setState] = useState<"loading" | "ready" | "hidden" | "busy">("loading");

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const config = await apiFetch<AuthConfig>("/api/public/auth/config");
        if (cancelled) return;
        if (!config.googleEnabled || !config.googleClientId) {
          logHidden("backend chưa đặt biến GOOGLE_CLIENT_ID (GET /api/public/auth/config trả googleEnabled=false)");
          setState("hidden");
          return;
        }
        if (!/^[\w-]+\.apps\.googleusercontent\.com$/.test(config.googleClientId)) {
          logHidden("GOOGLE_CLIENT_ID không đúng định dạng <số>-<chuỗi>.apps.googleusercontent.com", config.googleClientId);
          setState("hidden");
          return;
        }
        await loadScript();
        if (cancelled || !holder.current || !window.google) return;
        window.google.accounts.id.initialize({
          client_id: config.googleClientId,
          ux_mode: "popup",
          callback: async ({ credential }) => {
            setState("busy");
            try {
              const res = await apiFetch<GoogleAuthResponse>("/api/public/auth/google", {
                method: "POST",
                body: { credential },
              });
              if (res.newAccount) toast.success(`Chào mừng ${res.user.fullName}, tài khoản của bạn đã được tạo.`);
              signIn(res);
            } catch (e) {
              setState("ready");
              onError(errorMessage(e));
            }
          },
        });
        window.google.accounts.id.renderButton(holder.current, {
          type: "standard",
          theme: "outline",
          size: "large",
          text: mode === "signup" ? "signup_with" : "signin_with",
          shape: "rectangular",
          logo_alignment: "left",
          width: Math.min(400, Math.max(240, holder.current.offsetWidth)),
          locale: "vi",
        });
        setState("ready");
      } catch (e) {
        if (!cancelled) {
          logHidden("không tải được cấu hình hoặc thư viện Google Identity Services", e);
          setState("hidden");
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [mode, signIn, toast, onError]);

  if (state === "hidden") return null;

  // Chỉ hiện khi nút đã sẵn sàng, tránh khung trống hay đường kẻ thừa khi Google bị tắt
  return (
    <div className={`google-sign-in${state === "loading" ? " google-sign-in--pending" : ""}`} aria-busy={state === "busy"}>
      <div ref={holder} className="google-sign-in__button" />
      {state === "busy" && <p className="google-sign-in__status">Đang đăng nhập bằng Google...</p>}
      <AuthDivider label={dividerLabel} />
    </div>
  );
}
