"use client";

import { Plus, Stethoscope } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { StatusBadge } from "@/components/badge/Badge";
import { Button } from "@/components/button/Button";
import { Dialog } from "@/components/dialog/Dialog";
import { EmptyState } from "@/components/empty-state/EmptyState";
import { Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { Panel } from "@/components/panel/Panel";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { formatDateTime, formatMoney } from "@/lib/format";
import { CARE_MODE, PRIORITY, VET_STATUS } from "@/lib/labels";
import type { CareMode, ClinicView, PetDetail, Priority, VetRequestResult } from "@/types/api";
import "../../../detail-page.scss";

const AT_CENTER = ["IN_CARE", "IN_TREATMENT", "AVAILABLE", "PENDING_HANDOVER"];

export function VetTab({ pet, onChanged }: { pet: PetDetail; onChanged: () => void }) {
  const clinics = useApi<ClinicView[]>("/api/staff/vet-clinics");
  const mutate = useMutation();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ clinicId: "", reason: "", priority: "NEED_CHECK" as Priority, careMode: "OUTPATIENT" as CareMode });
  const canCreate = AT_CENTER.includes(pet.status);
  const warnApplications = pet.status === "AVAILABLE" || pet.status === "PENDING_HANDOVER";

  async function create() {
    if (!form.clinicId) throw new Error("Chọn cơ sở thú y.");
    const res = await mutate<VetRequestResult>(`/api/staff/pets/${pet.id}/vet-requests`, {
      body: { clinicId: Number(form.clinicId), reason: form.reason.trim(), priority: form.priority, careMode: form.careMode },
    });
    toast.success(
      res.cancelledApplications > 0
        ? `Đã tạo yêu cầu thú y và hủy ${res.cancelledApplications} đơn nhận nuôi.`
        : "Đã tạo yêu cầu thú y.",
    );
    setOpen(false);
    setForm({ ...form, reason: "" });
    onChanged();
  }

  return (
    <Panel
      title="Yêu cầu thú y"
      description="Mở yêu cầu thì thú chuyển sang Đang điều trị. Đóng yêu cầu cuối cùng thì thú về Đang chăm sóc."
      actions={
        canCreate && (
          <Button size="sm" icon={<Plus aria-hidden="true" />} onClick={() => setOpen(true)}>
            Tạo yêu cầu
          </Button>
        )
      }
    >
      {pet.vetRequests.length === 0 ? (
        <EmptyState icon={Stethoscope} title="Chưa có yêu cầu thú y" />
      ) : (
        <ul className="detail-page__links">
          {pet.vetRequests.map((v) => (
            <li key={v.id}>
              <Link href={`/staff/vet-requests/${v.id}`} className="detail-page__link">
                <span>
                  #{v.id} · {v.clinicName}
                </span>
                <StatusBadge map={VET_STATUS} value={v.status} />
                <StatusBadge map={PRIORITY} value={v.priority} />
                <span className="detail-page__link-meta">
                  {v.reason} · {CARE_MODE[v.careMode]} · {formatDateTime(v.requestedAt)}
                  {v.totalCost !== null && ` · ${formatMoney(v.totalCost)}`}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      <Dialog open={open} onClose={() => setOpen(false)} title="Tạo yêu cầu thú y" submitLabel="Tạo yêu cầu" onSubmit={create}>
        {warnApplications && (
          <Notice tone="warning">
            Thú đang mở nhận nuôi. Tạo yêu cầu thú y sẽ chuyển thú sang Đang điều trị và hủy các đơn đang chờ hoặc đã duyệt.
          </Notice>
        )}
        <Field label="Cơ sở thú y" required>
          <select value={form.clinicId} onChange={(e) => setForm({ ...form, clinicId: e.target.value })} required>
            <option value="">Chọn cơ sở</option>
            {clinics.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Lý do" required>
          <textarea rows={3} maxLength={1000} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} required />
        </Field>
        <div className="detail-page__form-grid">
          <Field label="Mức ưu tiên" required>
            <select value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value as Priority })}>
              {(Object.keys(PRIORITY) as Priority[]).map((p) => (
                <option key={p} value={p}>
                  {PRIORITY[p].label}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Hình thức" required hint="Nội trú: vị trí của thú đổi thành cơ sở thú y.">
            <select value={form.careMode} onChange={(e) => setForm({ ...form, careMode: e.target.value as CareMode })}>
              <option value="OUTPATIENT">{CARE_MODE.OUTPATIENT}</option>
              <option value="INPATIENT">{CARE_MODE.INPATIENT}</option>
            </select>
          </Field>
        </div>
      </Dialog>
    </Panel>
  );
}
