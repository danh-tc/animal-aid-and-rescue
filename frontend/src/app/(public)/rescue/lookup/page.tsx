"use client";

import { Search } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { type FormEvent, Suspense, useState } from "react";
import { StatusBadge } from "@/components/badge/Badge";
import { Button } from "@/components/button/Button";
import { Field } from "@/components/form-field/Field";
import { FormCard } from "@/components/form-card/FormCard";
import { Notice } from "@/components/notice/Notice";
import { Timeline } from "@/components/timeline/Timeline";
import { ApiRequestError, apiFetch, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { CASE_STATUS, REPORT_STATUS } from "@/lib/labels";
import type { ReportLookup } from "@/types/api";
import "../rescue-form.scss";

const STATUS_TEXT: Record<ReportLookup["status"], string> = {
  NEW: "Trung tâm đã nhận báo cáo và đang sắp xếp người xử lý.",
  PROCESSED: "Đội cứu hộ đã được phân công. Xem tiến độ của từng con vật bên dưới.",
  DUPLICATE: "Báo cáo trùng với một ca đang được xử lý, đội cứu hộ đã nắm thông tin.",
  INVALID: "Báo cáo không đủ thông tin hoặc không cần cứu hộ. Liên hệ trung tâm nếu bạn cần trao đổi thêm.",
};

function LookupForm() {
  const params = useSearchParams();
  const [code, setCode] = useState(params.get("code") ?? "");
  const [phone, setPhone] = useState("");
  const [result, setResult] = useState<ReportLookup | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!code.trim() || !phone.trim()) {
      setError("Nhập cả mã báo cáo và số điện thoại đã dùng khi báo.");
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      setResult(await apiFetch<ReportLookup>("/api/public/rescue-reports/lookup", {
        query: { code: code.trim(), phone: phone.trim() },
      }));
    } catch (e) {
      setError(
        e instanceof ApiRequestError && e.status === 404
          ? "Không tìm thấy báo cáo khớp mã và số điện thoại. Kiểm tra lại mã (dạng BC-XXXXXX) và số điện thoại."
          : errorMessage(e),
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <FormCard
      title="Tra cứu báo cáo cứu hộ"
      icon={Search}
      description="Nhập mã nhận được sau khi gửi báo cáo và số điện thoại đã dùng khi gửi."
      footer={
        <>
          Chưa gửi báo cáo? <Link href="/rescue">Báo thú cần cứu hộ</Link>
        </>
      }
    >
      <form className="stack" onSubmit={submit} noValidate>
        {error && <Notice tone="error">{error}</Notice>}
        <Field label="Mã báo cáo" required hint="Ví dụ BC-7K2M9Q">
          <input value={code} autoCapitalize="characters" onChange={(e) => setCode(e.target.value)} />
        </Field>
        <Field label="Số điện thoại" required>
          <input type="tel" inputMode="tel" autoComplete="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
        </Field>
        <Button type="submit" block loading={loading}>
          Tra cứu
        </Button>
      </form>

      {result && (
        <section className="rescue-form__result" aria-live="polite">
          <div className="rescue-form__receipt">
            <span className="rescue-form__receipt-label">Báo cáo {result.code}</span>
            <StatusBadge map={REPORT_STATUS} value={result.status} />
            <span className="rescue-form__receipt-time">Gửi lúc {formatDateTime(result.createdAt)}</span>
          </div>
          <p className="rescue-form__note">{STATUS_TEXT[result.status]}</p>
          {result.cases.length > 0 && (
            <Timeline
              entries={result.cases.map((c, i) => ({
                key: i,
                title: (
                  <>
                    {result.cases.length > 1 ? `Con vật ${i + 1}` : "Ca cứu hộ"}
                    <StatusBadge map={CASE_STATUS} value={c.status} />
                  </>
                ),
                time: c.closedAt ? `Kết thúc lúc ${formatDateTime(c.closedAt)}` : "Đang xử lý",
              }))}
            />
          )}
        </section>
      )}
    </FormCard>
  );
}

export default function LookupPage() {
  return (
    <div className="container">
      <Suspense>
        <LookupForm />
      </Suspense>
    </div>
  );
}
