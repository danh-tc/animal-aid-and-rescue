"use client";

import { useState } from "react";
import { Button } from "@/components/button/Button";
import { Checklist } from "@/components/checklist/Checklist";
import { CheckField } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { Panel } from "@/components/panel/Panel";
import { useToast } from "@/components/toast/Toast";
import { useMutation } from "@/hooks/useApi";
import { errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { EligibilityResult, PetDetail } from "@/types/api";
import "../../../detail-page.scss";

export function ChecklistTab({ pet, onChanged }: { pet: PetDetail; onChanged: () => void }) {
  const mutate = useMutation();
  const toast = useToast();
  const editable = pet.status === "IN_CARE" || pet.status === "IN_TREATMENT";
  const [form, setForm] = useState({
    vaccinated: pet.vaccinated,
    dewormed: pet.dewormed,
    sterilized: pet.sterilized,
    temperamentOk: pet.temperamentOk,
  });
  const [saving, setSaving] = useState(false);
  const [result, setResult] = useState<EligibilityResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function save() {
    setSaving(true);
    setError(null);
    try {
      const res = await mutate<EligibilityResult>(`/api/staff/pets/${pet.id}/eligibility`, { method: "PUT", body: form });
      setResult(res);
      toast.success(res.status === "AVAILABLE" ? "Thú đã sẵn sàng nhận nuôi và hiện trên trang công khai." : "Đã lưu checklist.");
      onChanged();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="detail-page">
      <div className="detail-page__main">
        <Panel
          title="Điều kiện nhận nuôi"
          description={
            pet.eligibilityCheckedAt
              ? `Kiểm tra lần cuối ${formatDateTime(pet.eligibilityCheckedAt)}`
              : "Đủ 6 mục thì thú tự chuyển sang Sẵn sàng nhận nuôi."
          }
        >
          {pet.checklist.allPassed && pet.status === "AVAILABLE" && (
            <Notice tone="success">Thú đang hiện trên trang công khai.</Notice>
          )}
          <Checklist items={result?.checklist.items ?? pet.checklist.items} />
        </Panel>
      </div>
      <aside className="detail-page__side">
        <Panel title="Cập nhật các mục do nhân viên xác nhận">
          <div className="stack">
            {!editable && (
              <Notice tone="info">Chỉ cập nhật được khi thú Đang chăm sóc hoặc Đang điều trị.</Notice>
            )}
            {error && <Notice tone="error">{error}</Notice>}
            {result && result.missing.length > 0 && (
              <Notice tone="warning" title="Còn thiếu">
                {result.missing.join(", ")}
              </Notice>
            )}
            <CheckField label="Đã tiêm phòng cơ bản" checked={form.vaccinated} disabled={!editable} onChange={(v) => setForm({ ...form, vaccinated: v })} />
            <CheckField label="Đã tẩy giun, ký sinh trùng" checked={form.dewormed} disabled={!editable} onChange={(v) => setForm({ ...form, dewormed: v })} />
            <CheckField
              label="Đã triệt sản"
              hint="Chưa đủ tuổi triệt sản thì mục này vẫn đạt."
              checked={form.sterilized}
              disabled={!editable}
              onChange={(v) => setForm({ ...form, sterilized: v })}
            />
            <CheckField label="Tính cách không hung dữ" checked={form.temperamentOk} disabled={!editable} onChange={(v) => setForm({ ...form, temperamentOk: v })} />
            <p className="form-field__hint">Sức khỏe, tuổi và thời gian cách ly do hệ thống tự tính.</p>
            <Button block loading={saving} disabled={!editable} onClick={save}>
              Lưu và kiểm tra
            </Button>
          </div>
        </Panel>
      </aside>
    </div>
  );
}
