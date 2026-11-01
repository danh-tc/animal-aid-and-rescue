"use client";

import { Check, Copy, LocateFixed, Siren } from "lucide-react";
import Link from "next/link";
import { type FormEvent, useState } from "react";
import { Button, LinkButton } from "@/components/button/Button";
import { FilePicker, filesValid } from "@/components/file-picker/FilePicker";
import { Field } from "@/components/form-field/Field";
import { FormCard } from "@/components/form-card/FormCard";
import { Notice } from "@/components/notice/Notice";
import { ApiRequestError, apiFetch, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { ReportReceipt, ReportRequest } from "@/types/api";
import "./rescue-form.scss";

interface Form {
  reporterName: string;
  reporterPhone: string;
  locationText: string;
  description: string;
}

type Errors = Partial<Record<keyof Form, string>>;

function validate(f: Form): Errors {
  const e: Errors = {};
  if (!f.reporterName.trim()) e.reporterName = "Nhập tên để đội cứu hộ xưng hô khi gọi lại.";
  if (!/^[0-9+]{9,15}$/.test(f.reporterPhone.trim())) e.reporterPhone = "Số điện thoại gồm 9 đến 15 chữ số.";
  if (!f.locationText.trim()) e.locationText = "Nhập địa chỉ hoặc mô tả vị trí để đội cứu hộ tìm đến.";
  if (!f.description.trim()) e.description = "Mô tả ngắn tình trạng của con vật.";
  return e;
}

export default function RescuePage() {
  const [form, setForm] = useState<Form>({ reporterName: "", reporterPhone: "", locationText: "", description: "" });
  const [coords, setCoords] = useState<{ latitude: number; longitude: number } | null>(null);
  const [locating, setLocating] = useState(false);
  const [photos, setPhotos] = useState<File[]>([]);
  const [errors, setErrors] = useState<Errors>({});
  const [touched, setTouched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [receipt, setReceipt] = useState<ReportReceipt | null>(null);
  const [copied, setCopied] = useState(false);

  function update<K extends keyof Form>(key: K, value: string) {
    const next = { ...form, [key]: value };
    setForm(next);
    if (touched) setErrors(validate(next));
  }

  function locate() {
    if (!navigator.geolocation) {
      setError("Trình duyệt không hỗ trợ lấy vị trí, hãy nhập địa chỉ bằng tay.");
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({
          latitude: Number(pos.coords.latitude.toFixed(6)),
          longitude: Number(pos.coords.longitude.toFixed(6)),
        });
        setLocating(false);
      },
      () => {
        setError("Không lấy được vị trí. Hãy cho phép truy cập vị trí hoặc nhập địa chỉ bằng tay.");
        setLocating(false);
      },
      { enableHighAccuracy: true, timeout: 10_000 },
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setTouched(true);
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) {
      document.getElementById(`rescue-${Object.keys(found)[0]}`)?.focus();
      return;
    }
    if (!filesValid(photos)) return;

    const report: ReportRequest = {
      reporterName: form.reporterName.trim(),
      reporterPhone: form.reporterPhone.trim(),
      locationText: form.locationText.trim(),
      description: form.description.trim(),
      latitude: coords?.latitude ?? null,
      longitude: coords?.longitude ?? null,
    };

    setSubmitting(true);
    setError(null);
    try {
      let body: unknown = report;
      if (photos.length > 0) {
        const data = new FormData();
        data.append("report", new Blob([JSON.stringify(report)], { type: "application/json" }));
        photos.forEach((p) => data.append("photos", p));
        body = data;
      }
      setReceipt(await apiFetch<ReportReceipt>("/api/public/rescue-reports", { method: "POST", body }));
      window.scrollTo({ top: 0 });
    } catch (e) {
      if (e instanceof ApiRequestError && Object.keys(e.fieldErrors).length > 0) setErrors(e.fieldErrors as Errors);
      setError(
        e instanceof ApiRequestError && e.status === 429
          ? "Số điện thoại này đã gửi nhiều báo cáo trong một giờ. Nếu khẩn cấp, vui lòng gọi trực tiếp cho trung tâm."
          : errorMessage(e),
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function copyCode() {
    if (!receipt) return;
    try {
      await navigator.clipboard.writeText(receipt.code);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      // trình duyệt chặn clipboard, người dùng vẫn đọc được mã
    }
  }

  if (receipt) {
    return (
      <div className="container">
        <FormCard title="Đã nhận báo cáo" icon={Check} description="Cảm ơn bạn. Đội cứu hộ sẽ liên hệ theo số điện thoại đã để lại.">
          <div className="rescue-form__receipt">
            <span className="rescue-form__receipt-label">Mã báo cáo của bạn</span>
            <span className="rescue-form__receipt-code">{receipt.code}</span>
            <span className="rescue-form__receipt-time">Gửi lúc {formatDateTime(receipt.createdAt)}</span>
            <Button variant="secondary" size="sm" icon={copied ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />} onClick={copyCode}>
              {copied ? "Đã sao chép" : "Sao chép mã"}
            </Button>
          </div>
          <p className="rescue-form__note">Hãy lưu mã này để trao đổi với trung tâm khi cần.</p>
          <div className="rescue-form__actions">
            <LinkButton href="/" variant="ghost">
              Về trang chủ
            </LinkButton>
            <Button
              onClick={() => {
                setReceipt(null);
                setForm({ ...form, locationText: "", description: "" });
                setPhotos([]);
                setCoords(null);
                setTouched(false);
              }}
            >
              Gửi báo cáo khác
            </Button>
          </div>
        </FormCard>
      </div>
    );
  }

  return (
    <div className="container">
      <FormCard
        wide
        title="Báo thú cần cứu hộ"
        icon={Siren}
        description={
          <>
            Không cần tài khoản. Nếu con vật đang gặp nguy hiểm tính mạng, hãy gọi trực tiếp cho trung tâm sau khi gửi.
            Đã có tài khoản nhân viên? <Link href="/login">Đăng nhập</Link>
          </>
        }
      >
        <form className="stack stack--lg" onSubmit={handleSubmit} noValidate>
          {error && <Notice tone="error">{error}</Notice>}

          <fieldset className="rescue-form__group">
            <legend className="rescue-form__legend">Người báo</legend>
            <div className="rescue-form__row">
              <Field label="Họ tên" required error={errors.reporterName}>
                <input
                  id="rescue-reporterName"
                  autoComplete="name"
                  value={form.reporterName}
                  onChange={(e) => update("reporterName", e.target.value)}
                />
              </Field>
              <Field label="Số điện thoại" required error={errors.reporterPhone}>
                <input
                  id="rescue-reporterPhone"
                  type="tel"
                  inputMode="tel"
                  autoComplete="tel"
                  value={form.reporterPhone}
                  onChange={(e) => update("reporterPhone", e.target.value)}
                />
              </Field>
            </div>
          </fieldset>

          <fieldset className="rescue-form__group">
            <legend className="rescue-form__legend">Vị trí và tình trạng</legend>
            <Field
              label="Địa chỉ, mô tả vị trí"
              required
              hint="Ví dụ: Hẻm 12 Nguyễn Trãi, Q.1, gần tiệm tạp hóa"
              error={errors.locationText}
            >
              <input
                id="rescue-locationText"
                autoComplete="street-address"
                value={form.locationText}
                onChange={(e) => update("locationText", e.target.value)}
              />
            </Field>
            <div className="rescue-form__locate">
              <Button
                variant="secondary"
                size="sm"
                icon={<LocateFixed aria-hidden="true" />}
                loading={locating}
                onClick={locate}
              >
                {coords ? "Lấy lại vị trí hiện tại" : "Dùng vị trí hiện tại"}
              </Button>
              <span className="rescue-form__coords" aria-live="polite">
                {coords ? `Đã ghi tọa độ ${coords.latitude}, ${coords.longitude}` : "Không bắt buộc, giúp đội cứu hộ tìm nhanh hơn."}
              </span>
            </div>
            <Field
              label="Tình trạng con vật"
              required
              hint="Loài, số lượng, có bị thương không, có hung dữ không."
              error={errors.description}
            >
              <textarea
                id="rescue-description"
                rows={4}
                maxLength={2000}
                value={form.description}
                onChange={(e) => update("description", e.target.value)}
              />
            </Field>
          </fieldset>

          <FilePicker label="Ảnh hiện trường" files={photos} onChange={setPhotos} />

          <Button type="submit" variant="accent" size="lg" block loading={submitting} icon={<Siren aria-hidden="true" />}>
            Gửi báo cáo
          </Button>
        </form>
      </FormCard>
    </div>
  );
}
