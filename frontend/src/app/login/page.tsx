"use client";

import { type FormEvent, useEffect, useState } from "react";
import { ApiRequestError, apiFetch } from "@/lib/api";
import { clearAuth, loadAuth, saveAuth } from "@/lib/auth-storage";
import type { AuthResponse } from "@/types/api";
import "./auth-form.scss";

const ROLE_LABEL = { ADOPTER: "Người nhận nuôi", STAFF: "Nhân viên", MANAGER: "Quản lý" } as const;

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [auth, setAuth] = useState<AuthResponse | null>(null);

  useEffect(() => {
    setAuth(loadAuth());
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const result = await apiFetch<AuthResponse>("/api/public/auth/login", {
        method: "POST",
        body: { email, password },
      });
      saveAuth(result);
      setAuth(result);
    } catch (e) {
      setError(e instanceof ApiRequestError ? e.message : "Không kết nối được tới máy chủ");
    } finally {
      setSubmitting(false);
    }
  }

  function handleLogout() {
    clearAuth();
    setAuth(null);
  }

  if (auth) {
    return (
      <div className="container">
        <div className="auth-form auth-form__session">
          <h1 className="auth-form__title">Xin chào, {auth.user.fullName}</h1>
          <p className="auth-form__hint">
            Đăng nhập với vai trò {ROLE_LABEL[auth.user.role]} ({auth.user.email})
          </p>
          <button type="button" className="btn btn--ghost" onClick={handleLogout}>
            Đăng xuất
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container">
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <h1 className="auth-form__title">Đăng nhập</h1>
        <p className="auth-form__hint">Tài khoản mẫu: staff1@aar.local, mật khẩu Password@123</p>

        {error && <p className="notice notice--error auth-form__error">{error}</p>}

        <div className="auth-form__field">
          <label className="auth-form__label" htmlFor="email">
            Email
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            className={`auth-form__input${error ? " auth-form__input--invalid" : ""}`}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="auth-form__field">
          <label className="auth-form__label" htmlFor="password">
            Mật khẩu
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            className={`auth-form__input${error ? " auth-form__input--invalid" : ""}`}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
          {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
        </button>
      </form>
    </div>
  );
}
