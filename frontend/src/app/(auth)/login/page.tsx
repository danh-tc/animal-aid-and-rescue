"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { type FormEvent, Suspense, useEffect, useState } from "react";
import { Button } from "@/components/button/Button";
import { Field } from "@/components/form-field/Field";
import { AuthShell } from "@/components/auth-shell/AuthShell";
import { Notice } from "@/components/notice/Notice";
import { PasswordInput } from "@/components/password-input/PasswordInput";
import { ApiRequestError, apiFetch, errorMessage } from "@/lib/api";
import { safeNext, useAuth } from "@/lib/auth-context";
import type { AuthResponse } from "@/types/api";

function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const { user, ready, signIn } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const next = params.get("next");

  // Đã đăng nhập thì chuyển thẳng tới trang đích
  useEffect(() => {
    if (ready && user) router.replace(safeNext(next, user.role));
  }, [ready, user, next, router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const result = await apiFetch<AuthResponse>("/api/public/auth/login", {
        method: "POST",
        body: { email: email.trim(), password },
      });
      signIn(result);
    } catch (e) {
      setError(
        e instanceof ApiRequestError && e.status === 401
          ? "Email hoặc mật khẩu không đúng. Kiểm tra lại rồi thử lần nữa."
          : errorMessage(e),
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthShell
      title="Chào mừng trở lại"
      description="Đăng nhập để nộp đơn nhận nuôi hoặc vào khu làm việc của nhân viên."
      footer={
        <>
          Chưa có tài khoản?{" "}
          <Link href={`/register${next ? `?next=${encodeURIComponent(next)}` : ""}`}>Đăng ký người nhận nuôi</Link>
        </>
      }
    >
      {params.get("expired") && !error && (
        <Notice tone="warning">Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.</Notice>
      )}
      {error && <Notice tone="error">{error}</Notice>}
      <form className="stack" onSubmit={handleSubmit}>
        <Field label="Email" required>
          <input
            type="email"
            autoComplete="email"
            inputMode="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </Field>
        <Field label="Mật khẩu" required>
          <PasswordInput autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <Button type="submit" block size="lg" loading={submitting}>
          Đăng nhập
        </Button>
        <p className="form-field__hint">Tài khoản mẫu: staff1@aar.local, manager@aar.local, mật khẩu Password@123</p>
      </form>
    </AuthShell>
  );
}

export default function LoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}
