"use client";

import { CalendarClock, ClipboardCheck, MapPin, Phone } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useState } from "react";
import { Badge } from "@/components/badge/Badge";
import { DataTable } from "@/components/data-table/DataTable";
import { Button } from "@/components/button/Button";
import { ChoiceGroup } from "@/components/choice-group/ChoiceGroup";
import { Dialog } from "@/components/dialog/Dialog";
import { EmptyState } from "@/components/empty-state/EmptyState";
import { FilePicker, filesValid } from "@/components/file-picker/FilePicker";
import { CheckField, Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Panel } from "@/components/panel/Panel";
import { Segmented } from "@/components/segmented/Segmented";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { useToast } from "@/components/toast/Toast";
import { Toolbar } from "@/components/toolbar/Toolbar";
import { useApi, useMutation } from "@/hooks/useApi";
import { useUrlState } from "@/hooks/useUrlState";
import { useAuth } from "@/lib/auth-context";
import { daysUntil, formatDate, petName, todayIso } from "@/lib/format";
import { CONTACT_METHOD, HANDOVER_STATUS } from "@/lib/labels";
import type { ContactMethod, ExceptionReport, FollowUpOutcome, FollowUpResult, HandoverView, ScheduledFollowUp } from "@/types/api";
import "./follow-up-board.scss";

type Range = "week" | "all";
const CRITERIA = [
  { key: "healthOk", label: "Sức khỏe của bé tốt" },
  { key: "housingOk", label: "Chỗ ở đúng như trong đơn" },
  { key: "commitmentOk", label: "Tiêm nhắc đúng lịch" },
  { key: "petPresent", label: "Bé vẫn ở với chủ nuôi" },
  { key: "ownerReachable", label: "Liên lạc được với chủ nuôi" },
] as const;
type CriteriaKey = (typeof CRITERIA)[number]["key"];
const ALL_OK: Record<CriteriaKey, boolean> = {
  healthOk: true,
  housingOk: true,
  commitmentOk: true,
  petPresent: true,
  ownerReachable: true,
};

function dueBadge(date: string) {
  const d = daysUntil(date);
  if (d < 0) return <Badge tone="danger">Quá hạn {-d} ngày</Badge>;
  if (d === 0) return <Badge tone="warning">Đến hạn hôm nay</Badge>;
  return <Badge tone="info">Còn {d} ngày</Badge>;
}

export default function FollowUpsPage() {
  const { user } = useAuth();
  const isManager = user?.role === "MANAGER";
  const [range, setRange] = useUrlState<Range>("range", "week");
  const focus = Number(useSearchParams().get("focus"));
  const { data, error, loading, reload } = useApi<ScheduledFollowUp[]>("/api/staff/follow-ups", {
    dueBefore: range === "week" ? todayIso(7) : undefined,
  });
  const mutate = useMutation();
  const toast = useToast();
  const [target, setTarget] = useState<ScheduledFollowUp | null>(null);
  const [result, setResult] = useState<FollowUpResult | null>(null);
  const [method, setMethod] = useState<ContactMethod>("PHONE");
  const [criteria, setCriteria] = useState(ALL_OK);
  const [reason, setReason] = useState("");
  const [files, setFiles] = useState<File[]>([]);

  function openFor(item: ScheduledFollowUp) {
    setTarget(item);
    setResult(null);

    setMethod(item.followUp.sequenceNo === 1 ? "PHONE" : "HOME_VISIT");
    setCriteria(ALL_OK);
    setReason("");
    setFiles([]);
  }

  const isAdditional = target?.followUp.additional ?? false;
  const resultChoices: { value: FollowUpResult; label: string; hint?: string }[] = [
    { value: "PASS", label: "Đạt", hint: "Bé ổn, không cần theo dõi thêm" },
    ...(isAdditional ? [] : [{ value: "WATCH" as const, label: "Cần theo dõi", hint: "Thêm một lần hậu kiểm bổ sung" }]),
    { value: "FAIL", label: "Không đạt", hint: "Chuyển sang chờ thu hồi" },
  ];

  async function submit() {
    if (!target) return;
    if (!result) throw new Error("Chọn kết quả hậu kiểm.");
    if (!resultChoices.some((c) => c.value === result)) throw new Error("Kết quả đã chọn không còn hợp lệ, hãy chọn lại.");
    if (result !== "PASS" && !reason.trim()) throw new Error("Chọn Cần theo dõi hoặc Không đạt thì phải ghi nhận xét.");
    if (!filesValid(files)) throw new Error("Có tệp lớn hơn 5MB.");
    const id = target.followUp.id;

    const data = new FormData();
    data.append("data", new Blob([JSON.stringify({ result, contactMethod: method, ...criteria, reason: reason.trim() || null })], {
      type: "application/json",
    }));
    files.forEach((f) => data.append("photos", f));
    const outcome = await mutate<FollowUpOutcome>(`/api/staff/follow-ups/${id}/result`, { body: data });
    const extra = outcome.additionalFollowUp
      ? ` Đã thêm lần hậu kiểm bổ sung ngày ${formatDate(outcome.additionalFollowUp.dueDate)}.`
      : "";
    toast.success(`Đã ghi kết quả. Bàn giao: ${HANDOVER_STATUS[outcome.handoverStatus].label}.${extra}`);
    setTarget(null);
    reload();
  }

  return (
    <>
      <PageHeader title="Hậu kiểm" description="Gọi điện hoặc đến thăm chủ nuôi, sau đó ghi kết quả theo 5 tiêu chí." />
      <Toolbar>
        <Segmented
          label="Khoảng thời gian"
          options={[
            { value: "week", label: "Đến hạn trong 7 ngày" },
            { value: "all", label: "Tất cả đã lên lịch" },
          ]}
          value={range}
          onChange={setRange}
        />
      </Toolbar>

      {error && <Notice tone="error">{error}</Notice>}
      {loading && !data && <Skeleton card lines={5} />}
      {data && data.length === 0 && (
        <div className="follow-up-board__empty">
          <EmptyState icon={ClipboardCheck} title="Không có lịch hậu kiểm" description="Không có lịch nào đến hạn trong khoảng đã chọn." />
        </div>
      )}
      {data && data.length > 0 && (
        <ul className="follow-up-board">
          {data.map((item) => {
            const f = item.followUp;
            return (
              <li
                key={f.id}
                className={`follow-up-board__card${f.id === focus ? " follow-up-board__card--focus" : ""}${daysUntil(f.dueDate) < 0 ? " follow-up-board__card--overdue" : ""}`}
              >
                <div className="follow-up-board__head">
                  <span className="follow-up-board__due">
                    <CalendarClock aria-hidden="true" />
                    {formatDate(f.dueDate)}
                  </span>
                  {dueBadge(f.dueDate)}
                </div>
                <h2 className="follow-up-board__title">
                  <Link href={`/staff/pets/${item.petId}?tab=handovers`}>{petName(item.petName, item.petCode)}</Link>
                </h2>
                <p className="follow-up-board__meta">
                  {item.petCode} · Lần {f.sequenceNo}
                  {f.additional && " (bổ sung)"}
                </p>
                <ul className="follow-up-board__contact">
                  <li>{item.ownerName}</li>
                  {item.ownerPhone && (
                    <li>
                      <a href={`tel:${item.ownerPhone}`}>
                        <Phone aria-hidden="true" />
                        {item.ownerPhone}
                      </a>
                    </li>
                  )}
                  {item.ownerAddress && (
                    <li>
                      <MapPin aria-hidden="true" />
                      {item.ownerAddress}
                    </li>
                  )}
                </ul>
                <Button block onClick={() => openFor(item)}>
                  Ghi kết quả
                </Button>
              </li>
            );
          })}
        </ul>
      )}

      <Dialog
        open={target !== null}
        onClose={() => setTarget(null)}
        title={target ? `Hậu kiểm ${petName(target.petName, target.petCode)}, lần ${target.followUp.sequenceNo}` : ""}
        submitLabel="Lưu kết quả"
        onSubmit={submit}
        size="lg"
      >
        <ChoiceGroup
          legend="Hình thức liên hệ"
          required
          value={method}
          onChange={setMethod}
          choices={(Object.keys(CONTACT_METHOD) as ContactMethod[]).map((m) => ({ value: m, label: CONTACT_METHOD[m] }))}
        />
        <fieldset className="follow-up-board__criteria">
          <legend className="choice-group__legend">Tiêu chí</legend>
          {CRITERIA.map((c) => (
            <CheckField
              key={c.key}
              label={c.label}
              checked={criteria[c.key]}
              onChange={(v) => setCriteria({ ...criteria, [c.key]: v })}
            />
          ))}
        </fieldset>
        <ChoiceGroup
          legend="Kết quả"
          required
          value={result}
          onChange={setResult}
          choices={resultChoices}
        />
        {isAdditional && (
          <p className="form-field__hint">
            Đây là lần hậu kiểm bổ sung nên chỉ chọn Đạt hoặc Không đạt. Nếu vẫn không liên lạc được chủ nuôi, chọn Không đạt.
          </p>
        )}
        <Field label="Nhận xét" required={result === "WATCH" || result === "FAIL"} hint="Bắt buộc khi Cần theo dõi hoặc Không đạt.">
          <textarea rows={3} maxLength={2000} value={reason} onChange={(e) => setReason(e.target.value)} />
        </Field>
        <FilePicker label="Ảnh thăm nhà (không bắt buộc)" files={files} onChange={setFiles} />
      </Dialog>
      {isManager && <RecallPanel />}
    </>
  );
}

type RecallRow = ExceptionReport["recallPending"][number];

function RecallPanel() {
  const { data, error, reload } = useApi<ExceptionReport>("/api/admin/reports/exceptions");
  const mutate = useMutation();
  const toast = useToast();
  const [target, setTarget] = useState<RecallRow | null>(null);
  const [reason, setReason] = useState("");
  const now = Date.now();

  async function closeRecall() {
    if (!target) return;
    if (!reason.trim()) throw new Error("Ghi lý do đóng thu hồi.");
    await mutate<HandoverView>(`/api/staff/handovers/${target.handoverId}/close-recall`, { body: { reason: reason.trim() } });
    toast.success(`Đã đóng thu hồi. ${petName(target.petName, target.petCode)} giữ trạng thái Đã nhận nuôi.`);
    setTarget(null);
    reload();
  }

  return (
    <Panel title="Bàn giao chờ thu hồi" description="Các bàn giao có hậu kiểm Không đạt. Liên hệ chủ nuôi để thu hồi thú, nếu quá thời gian chờ vẫn không thu hồi được thì Quản lý đóng thu hồi." flush>
      {error && <Notice tone="error">{error}</Notice>}
      {data && data.recallPending.length === 0 && <EmptyState icon={ClipboardCheck} title="Không có bàn giao chờ thu hồi" />}
      {data && data.recallPending.length > 0 && (
        <DataTable
          caption="Bàn giao chờ thu hồi"
          rows={data.recallPending}
          rowKey={(r) => r.handoverId}
          columns={[
            {
              key: "pet",
              header: "Thú",
              render: (r) => <Link href={`/staff/pets/${r.petId}?tab=handovers`}>{petName(r.petName, r.petCode)}</Link>,
            },
            { key: "owner", header: "Chủ nuôi", render: (r) => `${r.ownerName}${r.ownerPhone ? ` · ${r.ownerPhone}` : ""}` },
            { key: "failed", header: "Ngày Không đạt", render: (r) => (r.failedAt ? formatDate(r.failedAt) : "—"), hideOnMobile: true },
            {
              key: "action",
              header: "Đóng thu hồi",
              render: (r) => {
                const ready = !!r.closableFrom && new Date(r.closableFrom).getTime() <= now;
                return ready ? (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => {
                      setReason("");
                      setTarget(r);
                    }}
                  >
                    Đóng thu hồi
                  </Button>
                ) : (
                  `Đóng được từ ${r.closableFrom ? formatDate(r.closableFrom) : "—"}`
                );
              },
            },
          ]}
        />
      )}
      <Dialog
        open={target !== null}
        onClose={() => setTarget(null)}
        title="Đóng thu hồi?"
        description="Bàn giao chuyển sang Đã đóng, thú vẫn ở trạng thái Đã nhận nuôi. Nếu người này nộp đơn sau, nhân viên sẽ thấy cảnh báo."
        submitLabel="Đóng thu hồi"
        submitVariant="danger"
        onSubmit={closeRecall}
      >
        <Field label="Lý do" required>
          <textarea rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} required />
        </Field>
      </Dialog>
    </Panel>
  );
}
