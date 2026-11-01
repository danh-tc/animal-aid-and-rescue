"use client";

import { UserPlus } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { type FormEvent, Suspense, useEffect, useState } from "react";
import { Button } from "@/components/button/Button";
import { Field } from "@/components/form-field/Field";
import { FormCard } from "@/components/form-card/FormCard";
import { Notice } from "@/components/notice/Notice";
import { PasswordInput } from "@/components/password-input/PasswordInput";
import { ApiRequestError, apiFetch, errorMessage } from "@/lib/api";
import { safeNext, useAuth } from "@/lib/auth-context";
import type { AuthResponse } from "@/types/api";

interface Form {
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

type Errors = Partial<Record<keyof Form, string>>;

function validate(f: Form): Errors {
  const errors: Errors = {};
  if (!f.fullName.trim()) errors.fullName = "Nhập họ tên để nhân viên liên hệ.";
  if (!/^\S+@\S+\.\S+$/.test(f.email.trim())) errors.email = "Email chưa đúng định dạng, ví dụ ten@gmail.com.";
  if (f.phone && !/^[0-9+]{9,15}$/.test(f.phone.trim())) errors.phone = "Số điện thoại gồm 9 đến 15 chữ số.";
  if (f.password.length < 8 || f.password.length > 72) errors.password = "Mật khẩu dài từ 8 đến 72 ký tự.";
  return errors;
}

function RegisterForm() {
  const router = useRouter();
  const params = useSearchParams();
  const { user, ready, signIn } = useAuth();
  const [form, setForm] = useState<Form>({ fullName: "", email: "", phone: "", password: "" });
  const [errors, setErrors] = useState<Errors>({});
  const [touched, setTouched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const next = params.get("next");

  useEffect(() => {
    if (ready && user) router.replace(safeNext(next, user.role));
  }, [ready, user, next, router]);

  function update<K extends keyof Form>(key: K, value: Form[K]) {
    const nextForm = { ...form, [key]: value };
    setForm(nextForm);
    if (touched) setErrors(validate(nextForm));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setTouched(true);
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) {
      document.getElementById(`reg-${Object.keys(found)[0]}`)?.focus();
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const result = await apiFetch<AuthResponse>("/api/public/auth/register", {
        method: "POST",
        body: {
          fullName: form.fullName.trim(),
          email: form.email.trim(),
          phone: form.phone.trim() || null,
          password: form.password,
        },
      });
      signIn(result);
    } catch (e) {
      if (e instanceof ApiRequestError && Object.keys(e.fieldErrors).length > 0) {
        setErrors(e.fieldErrors as Errors);
      }
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <FormCard
      title="Tạo tài khoản"
      icon={UserPlus}
      description="Tài khoản người nhận nuôi dùng để nộp đơn và theo dõi lịch hậu kiểm."
      footer={
        <>
          Đã có tài khoản? <Link href={`/login${next ? `?next=${encodeURIComponent(next)}` : ""}`}>Đăng nhập</Link>
        </>
      }
    >
      <form className="stack" onSubmit={handleSubmit} noValidate>
        {error && <Notice tone="error">{error}</Notice>}
        <Field label="Họ và tên" required error={errors.fullName}>
          <input
            id="reg-fullName"
            autoComplete="name"
            value={form.fullName}
            onChange={(e) => update("fullName", e.target.value)}
          />
        </Field>
        <Field label="Email" required error={errors.email}>
          <input
            id="reg-email"
            type="email"
            autoComplete="email"
            inputMode="email"
            value={form.email}
            onChange={(e) => update("email", e.target.value)}
          />
        </Field>
        <Field label="Số điện thoại" hint="Nhân viên gọi để xác minh trước khi duyệt đơn." error={errors.phone}>
          <input
            id="reg-phone"
            type="tel"
            autoComplete="tel"
            inputMode="tel"
            value={form.phone}
            onChange={(e) => update("phone", e.target.value)}
          />
        </Field>
        <Field label="Mật khẩu" required hint="Từ 8 đến 72 ký tự." error={errors.password}>
          <PasswordInput
            id="reg-password"
            autoComplete="new-password"
            value={form.password}
            onChange={(e) => update("password", e.target.value)}
          />
        </Field>
        <Button type="submit" block size="lg" loading={submitting}>
          Tạo tài khoản
        </Button>
      </form>
    </FormCard>
  );
}

export default function RegisterPage() {
  return (
    <div className="container">
      <Suspense>
        <RegisterForm />
      </Suspense>
    </div>
  );
}
