"use client";

import { PawPrint, Undo2 } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { StatusBadge } from "@/components/badge/Badge";
import { Button } from "@/components/button/Button";
import { DetailList } from "@/components/detail-list/DetailList";
import { Dialog } from "@/components/dialog/Dialog";
import { EmptyState } from "@/components/empty-state/EmptyState";
import { Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { Panel } from "@/components/panel/Panel";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { Timeline } from "@/components/timeline/Timeline";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { formatDate, formatDateTime, formatMoney } from "@/lib/format";
import { FOLLOW_UP_RESULT, FOLLOW_UP_STATUS, HANDOVER_STATUS, PRIORITY } from "@/lib/labels";
import type { HandoverView, PetDetail, Priority, ReturnResult } from "@/types/api";
import "../../../detail-page.scss";

const RETURNABLE = ["IN_PROGRESS", "COMPLETED", "RECALL_PENDING"];

/** Bàn giao, hậu kiểm và S10: tiếp nhận trả lại hoặc thu hồi */
export function HandoverTab({ pet, onChanged }: { pet: PetDetail; onChanged: () => void }) {
  const { data, error, loading, reload } = useApi<HandoverView[]>(`/api/staff/pets/${pet.id}/handovers`);
  const mutate = useMutation();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ reason: "", priority: "NEED_CHECK" as Priority, assessment: "" });
  const current = data?.find((h) => RETURNABLE.includes(h.status));
  const canReturn = pet.status === "ADOPTED" && !!current;

  async function submitReturn() {
    const res = await mutate<ReturnResult>(`/api/staff/pets/${pet.id}/returns`, {
      body: { reason: form.reason.trim(), priority: form.priority, initialAssessment: form.assessment.trim() || null },
    });
    toast.success(
      `Đã nhận lại thú vào khu cách ly${res.cancelledFollowUps > 0 ? `, hủy ${res.cancelledFollowUps} lịch hậu kiểm` : ""}.`,
    );
    setOpen(false);
    reload();
    onChanged();
  }

  return (
    <div className="stack stack--lg">
      {current?.status === "RECALL_PENDING" && (
        <Notice tone="warning" title="Hậu kiểm không đạt, đang chờ thu hồi">
          Liên hệ chủ nuôi để thu hồi rồi bấm Tiếp nhận trả lại.
        </Notice>
      )}
      <Panel
        title="Bàn giao và hậu kiểm"
        actions={
          canReturn && (
            <Button variant="secondary" size="sm" icon={<Undo2 aria-hidden="true" />} onClick={() => setOpen(true)}>
              Tiếp nhận trả lại
            </Button>
          )
        }
      >
        {error && <Notice tone="error">{error}</Notice>}
        {loading && !data && <Skeleton lines={4} />}
        {data?.length === 0 && <EmptyState icon={PawPrint} title="Thú chưa từng được bàn giao" />}
        <div className="stack stack--lg">
          {data?.map((h) => (
            <article key={h.id} className="stack">
              <h3 className="detail-page__form-section">
                Bàn giao #{h.id} <StatusBadge map={HANDOVER_STATUS} value={h.status} />
              </h3>
              <DetailList
                items={[
                  { label: "Chủ nuôi", value: `${h.ownerName}${h.ownerPhone ? ` · ${h.ownerPhone}` : ""}` },
                  { label: "Ngày bàn giao", value: formatDateTime(h.handedOverAt) },
                  { label: "Khoản ủng hộ", value: formatMoney(h.donationAmount) },
                  { label: "Đơn nhận nuôi", value: <Link href={`/staff/applications/${h.applicationId}`}>Đơn #{h.applicationId}</Link> },
                  ...(h.note ? [{ label: "Ghi chú", value: h.note, wide: true }] : []),
                ]}
              />
              <Timeline
                entries={h.followUps.map((f) => ({
                  key: f.id,
                  title: (
                    <>
                      Hậu kiểm lần {f.sequenceNo}
                      {f.additional && " (bổ sung)"}
                      {f.result ? (
                        <StatusBadge map={FOLLOW_UP_RESULT} value={f.result} />
                      ) : (
                        <StatusBadge map={FOLLOW_UP_STATUS} value={f.status} />
                      )}
                    </>
                  ),
                  time: `Hạn ${formatDate(f.dueDate)}`,
                  body: f.performedAt ? `Thực hiện ${formatDateTime(f.performedAt)}${f.reason ? `. ${f.reason}` : ""}` : undefined,
                }))}
              />
            </article>
          ))}
        </div>
      </Panel>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title="Tiếp nhận thú trả lại"
        description="Thú quay về trung tâm ở khu cách ly. Các lịch hậu kiểm còn lại sẽ bị hủy."
        submitLabel="Tiếp nhận"
        onSubmit={submitReturn}
      >
        <Field label="Lý do" required hint="Chủ nuôi trả lại, hoặc thu hồi do hậu kiểm không đạt.">
          <textarea rows={3} maxLength={1000} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} required />
        </Field>
        <Field label="Mức ưu tiên" required>
          <select value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value as Priority })}>
            {(Object.keys(PRIORITY) as Priority[]).map((p) => (
              <option key={p} value={p}>
                {PRIORITY[p].label}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Đánh giá ban đầu">
          <textarea rows={2} maxLength={2000} value={form.assessment} onChange={(e) => setForm({ ...form, assessment: e.target.value })} />
        </Field>
      </Dialog>
    </div>
  );
}
