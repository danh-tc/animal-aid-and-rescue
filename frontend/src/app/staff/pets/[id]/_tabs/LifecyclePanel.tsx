"use client";

import { Flag, Search } from "lucide-react";
import { useState } from "react";
import { Badge, StatusBadge } from "@/components/badge/Badge";
import { Button } from "@/components/button/Button";
import { ChoiceGroup } from "@/components/choice-group/ChoiceGroup";
import { Dialog } from "@/components/dialog/Dialog";
import { Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { useToast } from "@/components/toast/Toast";
import { useMutation } from "@/hooks/useApi";
import { formatDateTime } from "@/lib/format";
import { END_EVENT, PET_STATUS, PRIORITY } from "@/lib/labels";
import type { EndEventResult, EndEventType, EndEventView, FoundResult, PetDetail, Priority } from "@/types/api";

const AT_CENTER = ["IN_CARE", "IN_TREATMENT", "AVAILABLE", "PENDING_HANDOVER"];

/** Nút ghi sự kiện kết thúc (mục 2.1.6) và tìm lại thú thất lạc, đặt ở đầu hồ sơ thú. */
export function LifecycleActions({ pet, onChanged }: { pet: PetDetail; onChanged: () => void }) {
  const mutate = useMutation();
  const toast = useToast();
  const [open, setOpen] = useState<"end" | "found" | null>(null);
  const [type, setType] = useState<EndEventType | null>(null);
  const [form, setForm] = useState({ occurredAt: "", detail: "", destination: "" });
  const [found, setFound] = useState({ reason: "", priority: "NEED_CHECK" as Priority, assessment: "" });
  const adopted = pet.status === "ADOPTED";
  const canEnd = AT_CENTER.includes(pet.status) || adopted;

  async function record() {
    if (!type) throw new Error("Chọn loại sự kiện.");
    if (type === "LOST" && !form.detail.trim()) throw new Error("Ghi nơi và hoàn cảnh mất.");
    if (type === "TRANSFERRED" && (!form.destination.trim() || !form.detail.trim()))
      throw new Error("Chuyển cơ sở phải ghi nơi nhận và lý do.");
    const res = await mutate<EndEventResult>(`/api/staff/pets/${pet.id}/end-events`, {
      body: {
        eventType: type,
        occurredAt: form.occurredAt ? new Date(form.occurredAt).toISOString() : null,
        detail: form.detail.trim() || null,
        destinationOrg: type === "TRANSFERRED" ? form.destination.trim() : null,
      },
    });
    const closed = [
      res.cancelledVetRequests && `${res.cancelledVetRequests} yêu cầu thú y`,
      res.cancelledApplications && `${res.cancelledApplications} đơn nhận nuôi`,
      res.cancelledFollowUps && `${res.cancelledFollowUps} lịch hậu kiểm`,
    ].filter(Boolean);
    toast.success(
      `Đã ghi nhận ${END_EVENT[res.event.eventType].label.toLowerCase()}.${closed.length ? ` Đã đóng ${closed.join(", ")}.` : ""}`,
    );
    setOpen(null);
    onChanged();
  }

  async function markFound() {
    await mutate<FoundResult>(`/api/staff/pets/${pet.id}/found`, {
      body: { reason: found.reason.trim(), priority: found.priority, initialAssessment: found.assessment.trim() || null },
    });
    toast.success("Đã tiếp nhận lại thú vào khu cách ly.");
    setOpen(null);
    onChanged();
  }

  return (
    <>
      {pet.status === "LOST" && (
        <Button icon={<Search aria-hidden="true" />} onClick={() => setOpen("found")}>
          Tìm lại được
        </Button>
      )}
      {canEnd && (
        <Button
          variant="ghost"
          icon={<Flag aria-hidden="true" />}
          onClick={() => {
            setType(null);
            setForm({ occurredAt: "", detail: "", destination: "" });
            setOpen("end");
          }}
        >
          Sự kiện kết thúc
        </Button>
      )}

      <Dialog
        open={open === "end"}
        onClose={() => setOpen(null)}
        title="Ghi sự kiện kết thúc"
        description="Hồ sơ và lịch sử được giữ nguyên. Hệ thống hủy yêu cầu thú y đang mở và đóng các đơn đang chờ hoặc đã duyệt."
        submitLabel="Ghi nhận"
        submitVariant="danger"
        onSubmit={record}
        size="lg"
      >
        {adopted && (
          <Notice tone="info">
            Thú đã nhận nuôi chỉ ghi được khi còn trong thời gian hậu kiểm. Lịch hậu kiểm còn lại sẽ bị hủy.
          </Notice>
        )}
        <ChoiceGroup
          legend="Sự kiện"
          required
          value={type}
          onChange={setType}
          choices={[
            { value: "DECEASED", label: "Qua đời" },
            { value: "LOST", label: "Thất lạc", hint: "Tìm lại được thì tạo lượt tiếp nhận mới" },
            ...(adopted ? [] : [{ value: "TRANSFERRED" as const, label: "Chuyển cơ sở", hint: "Sang tổ chức khác" }]),
          ]}
        />
        <Field label="Thời điểm" hint="Để trống nếu là bây giờ.">
          <input type="datetime-local" value={form.occurredAt} onChange={(e) => setForm({ ...form, occurredAt: e.target.value })} />
        </Field>
        {type === "TRANSFERRED" && (
          <Field label="Nơi nhận" required>
            <input maxLength={255} value={form.destination} onChange={(e) => setForm({ ...form, destination: e.target.value })} />
          </Field>
        )}
        <Field
          label={type === "LOST" ? "Nơi và hoàn cảnh mất" : type === "TRANSFERRED" ? "Lý do chuyển" : "Chi tiết"}
          required={type === "LOST" || type === "TRANSFERRED"}
        >
          <textarea rows={3} maxLength={2000} value={form.detail} onChange={(e) => setForm({ ...form, detail: e.target.value })} />
        </Field>
      </Dialog>

      <Dialog
        open={open === "found"}
        onClose={() => setOpen(null)}
        title="Tìm lại thú thất lạc"
        description="Tạo lượt tiếp nhận mới trên hồ sơ cũ. Thú vào khu cách ly, trạng thái Đang chăm sóc."
        submitLabel="Tiếp nhận"
        onSubmit={markFound}
      >
        <Field label="Lý do, nơi tìm thấy" required>
          <textarea rows={3} maxLength={1000} value={found.reason} onChange={(e) => setFound({ ...found, reason: e.target.value })} required />
        </Field>
        <Field label="Mức ưu tiên" required>
          <select value={found.priority} onChange={(e) => setFound({ ...found, priority: e.target.value as Priority })}>
            {(Object.keys(PRIORITY) as Priority[]).map((p) => (
              <option key={p} value={p}>
                {PRIORITY[p].label}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Đánh giá ban đầu">
          <textarea rows={2} maxLength={2000} value={found.assessment} onChange={(e) => setFound({ ...found, assessment: e.target.value })} />
        </Field>
      </Dialog>
    </>
  );
}

/** Thông báo đầu hồ sơ khi thú ở trạng thái kết thúc hoặc thất lạc. */
export function EndEventNotice({ pet, events }: { pet: PetDetail; events: EndEventView[] | null }) {
  if (!["DECEASED", "LOST", "TRANSFERRED"].includes(pet.status)) return null;
  const last = events?.[0];
  return (
    <Notice tone={pet.status === "LOST" ? "warning" : "info"} title={`Thú ${PET_STATUS[pet.status].label.toLowerCase()}`}>
      {last ? (
        <>
          <StatusBadge map={END_EVENT} value={last.eventType} /> {formatDateTime(last.occurredAt)}
          {last.destinationOrg && <> · Nơi nhận: {last.destinationOrg}</>}
          {last.detail && <> · {last.detail}</>}
        </>
      ) : (
        <Badge tone="neutral">Chưa có chi tiết</Badge>
      )}
    </Notice>
  );
}
