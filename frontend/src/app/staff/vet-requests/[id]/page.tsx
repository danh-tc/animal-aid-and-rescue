"use client";

import { CheckCircle2, Play, Upload, XCircle } from "lucide-react";
import { useParams } from "next/navigation";
import { useState } from "react";
import { Badge, StatusBadge } from "@/components/badge/Badge";
import { AttachmentList } from "@/components/attachment-list/AttachmentList";
import { Button, LinkButton } from "@/components/button/Button";
import { ChoiceGroup } from "@/components/choice-group/ChoiceGroup";
import { DetailList } from "@/components/detail-list/DetailList";
import { Dialog } from "@/components/dialog/Dialog";
import { FilePicker, filesValid } from "@/components/file-picker/FilePicker";
import { Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Panel } from "@/components/panel/Panel";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { formatDateTime, formatMoney } from "@/lib/format";
import { CARE_MODE, PET_STATUS, PRIORITY, VET_STATUS } from "@/lib/labels";
import type { AttachmentView, VetRequestResult, VetRequestView } from "@/types/api";
import "../../detail-page.scss";

/** S06: Chi tiết yêu cầu thú y: bắt đầu điều trị, ghi kết quả, hủy, đính kèm phiếu khám */
export default function VetRequestDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: v, error, loading, reload } = useApi<VetRequestView>(`/api/staff/vet-requests/${id}`);
  const mutate = useMutation();
  const toast = useToast();
  const [dialog, setDialog] = useState<"complete" | "cancel" | "upload" | null>(null);
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState({ diagnosis: "", treatment: "", cost: "", aftercare: "" });
  const [healthOk, setHealthOk] = useState<"yes" | "no" | null>(null);
  const [reason, setReason] = useState("");
  const [incurredCost, setIncurredCost] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [completeFiles, setCompleteFiles] = useState<File[]>([]);
  const attachments = useApi<AttachmentView[]>(`/api/staff/vet-requests/${id}/attachments`);

  if (loading && !v) return <Skeleton card lines={6} />;
  if (!v) return <Notice tone="error">{error}</Notice>;
  const open = v.status === "PENDING" || v.status === "IN_TREATMENT";

  function notify(res: VetRequestResult, message: string) {
    const extra = res.cancelledApplications > 0 ? ` Đã hủy ${res.cancelledApplications} đơn nhận nuôi.` : "";
    toast.success(`${message} Thú hiện ở trạng thái ${PET_STATUS[res.petStatus].label.toLowerCase()}.${extra}`);
  }

  async function start() {
    setBusy(true);
    try {
      notify(await mutate<VetRequestResult>(`/api/staff/vet-requests/${id}/start`), "Đã bắt đầu điều trị.");
      reload();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Không thực hiện được thao tác.");
    } finally {
      setBusy(false);
    }
  }

  async function complete() {
    const cost = Number(done.cost);
    if (done.cost === "" || !(cost >= 0) || !Number.isInteger(cost)) throw new Error("Tổng chi phí phải là số nguyên không âm (VND).");
    if (!healthOk) throw new Error("Chọn kết luận sức khỏe sau lần khám.");
    if (!filesValid(completeFiles)) throw new Error("Có tệp lớn hơn 5MB.");
    // Mục 5.2: multipart gồm phần "data" (JSON kết quả) và "files" (phiếu khám) trong cùng một yêu cầu
    const data = new FormData();
    const result = {
      diagnosisSummary: done.diagnosis.trim() || null,
      treatmentSummary: done.treatment.trim() || null,
      totalCost: cost,
      aftercareInstructions: done.aftercare.trim() || null,
      healthOk: healthOk === "yes",
    };
    data.append("data", new Blob([JSON.stringify(result)], { type: "application/json" }));
    completeFiles.forEach((f) => data.append("files", f));
    const res = await mutate<VetRequestResult>(`/api/staff/vet-requests/${id}/complete`, { body: data });
    notify(res, "Đã ghi kết quả khám.");
    setCompleteFiles([]);
    setDialog(null);
    reload();
    attachments.reload();
  }

  async function cancel() {
    const cost = incurredCost.trim() === "" ? 0 : Number(incurredCost);
    if (!Number.isInteger(cost) || cost < 0) throw new Error("Chi phí đã phát sinh phải là số nguyên không âm.");
    const res = await mutate<VetRequestResult>(`/api/staff/vet-requests/${id}/cancel`, {
      body: { reason: reason.trim(), incurredCost: cost },
    });
    notify(res, "Đã hủy yêu cầu.");
    setDialog(null);
    reload();
  }

  async function upload() {
    if (files.length === 0) throw new Error("Chọn ít nhất một tệp.");
    if (!filesValid(files)) throw new Error("Có tệp lớn hơn 5MB.");
    const data = new FormData();
    files.forEach((f) => data.append("files", f));
    const saved = await mutate<AttachmentView[]>(`/api/staff/vet-requests/${id}/attachments`, { body: data });
    attachments.reload();
    toast.success(`Đã tải lên ${saved.length} tệp.`);
    setFiles([]);
    setDialog(null);
  }

  return (
    <>
      <PageHeader
        back={{ href: "/staff/vet-requests", label: "Yêu cầu thú y" }}
        title={`Yêu cầu thú y #${v.id}`}
        meta={
          <>
            <StatusBadge map={VET_STATUS} value={v.status} />
            <StatusBadge map={PRIORITY} value={v.priority} />
            <span>{v.clinicName}</span>
          </>
        }
        actions={
          open && (
            <>
              <Button variant="ghost" icon={<XCircle aria-hidden="true" />} onClick={() => setDialog("cancel")}>
                Hủy yêu cầu
              </Button>
              {v.status === "PENDING" && (
                <Button variant="secondary" icon={<Play aria-hidden="true" />} loading={busy} onClick={start}>
                  Bắt đầu điều trị
                </Button>
              )}
              <Button icon={<CheckCircle2 aria-hidden="true" />} onClick={() => setDialog("complete")}>
                Ghi kết quả
              </Button>
            </>
          )
        }
      />

      <div className="detail-page">
        <div className="detail-page__main">
          <Panel title="Yêu cầu">
            <DetailList
              items={[
                { label: "Lý do", value: v.reason, wide: true },
                { label: "Hình thức", value: CARE_MODE[v.careMode] },
                { label: "Tạo lúc", value: formatDateTime(v.requestedAt) },
              ]}
            />
          </Panel>
          {(v.status === "COMPLETED" || v.status === "CANCELLED") && (
            <Panel title={v.status === "COMPLETED" ? "Kết quả khám" : "Đã hủy"}>
              <DetailList
                items={
                  v.status === "COMPLETED"
                    ? [
                        { label: "Chẩn đoán", value: v.diagnosisSummary, wide: true },
                        { label: "Điều trị", value: v.treatmentSummary, wide: true },
                        { label: "Hướng dẫn chăm sóc", value: v.aftercareInstructions, wide: true },
                        {
                          label: "Kết luận",
                          value: v.healthOk ? <Badge tone="success">Sức khỏe ổn</Badge> : <Badge tone="warning">Chưa ổn</Badge>,
                        },
                        { label: "Tổng chi phí", value: formatMoney(v.totalCost) },
                        { label: "Hoàn tất lúc", value: formatDateTime(v.completedAt) },
                      ]
                    : [
                        { label: "Lý do hủy", value: v.cancelReason, wide: true },
                        { label: "Chi phí đã phát sinh", value: formatMoney(v.totalCost ?? 0) },
                      ]
                }
              />
            </Panel>
          )}
        </div>
        <aside className="detail-page__side">
          <Panel title="Thú">
            <LinkButton href={`/staff/pets/${v.petId}?tab=vet`} variant="secondary" block>
              Mở hồ sơ thú #{v.petId}
            </LinkButton>
          </Panel>
          <Panel
            title="Phiếu khám, hóa đơn"
            actions={
              <Button size="sm" variant="ghost" icon={<Upload aria-hidden="true" />} onClick={() => setDialog("upload")}>
                Tải lên
              </Button>
            }
          >
            {attachments.data && <AttachmentList items={attachments.data} />}
          </Panel>
        </aside>
      </div>

      <Dialog open={dialog === "complete"} onClose={() => setDialog(null)} title="Ghi kết quả khám" submitLabel="Hoàn tất" onSubmit={complete} size="lg">
        <Field label="Chẩn đoán">
          <textarea rows={2} maxLength={2000} value={done.diagnosis} onChange={(e) => setDone({ ...done, diagnosis: e.target.value })} />
        </Field>
        <Field label="Điều trị">
          <textarea rows={2} maxLength={2000} value={done.treatment} onChange={(e) => setDone({ ...done, treatment: e.target.value })} />
        </Field>
        <Field label="Hướng dẫn chăm sóc sau khám">
          <textarea rows={2} maxLength={2000} value={done.aftercare} onChange={(e) => setDone({ ...done, aftercare: e.target.value })} />
        </Field>
        <Field label="Tổng chi phí (VND)" required hint="Bắt buộc khi hoàn tất, nhập 0 nếu miễn phí.">
          <input type="number" min={0} step={1000} inputMode="numeric" value={done.cost} onChange={(e) => setDone({ ...done, cost: e.target.value })} required />
        </Field>
        <ChoiceGroup
          legend="Kết luận sức khỏe"
          required
          columns={2}
          value={healthOk}
          onChange={setHealthOk}
          choices={[
            { value: "yes", label: "Sức khỏe ổn", hint: "Đạt mục sức khỏe trong checklist nhận nuôi" },
            { value: "no", label: "Chưa ổn", hint: "Cần theo dõi hoặc tái khám" },
          ]}
        />
        <FilePicker
          label="Phiếu khám (không bắt buộc)"
          files={completeFiles}
          onChange={setCompleteFiles}
          accept="image/jpeg,image/png,image/webp,application/pdf"
          hint="JPG, PNG, WEBP hoặc PDF, tối đa 5 tệp, mỗi tệp không quá 5MB."
        />
      </Dialog>

      <Dialog open={dialog === "cancel"} onClose={() => setDialog(null)} title="Hủy yêu cầu thú y?" submitLabel="Hủy yêu cầu" submitVariant="danger" onSubmit={cancel}>
        <Field label="Lý do" required>
          <textarea rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} required />
        </Field>
        <Field label="Chi phí đã phát sinh (VND)" hint="Để trống nếu chưa phát sinh. Số tiền này vẫn tính vào tổng chi phí điều trị.">
          <input type="number" min={0} step={1000} inputMode="numeric" value={incurredCost} onChange={(e) => setIncurredCost(e.target.value)} />
        </Field>
      </Dialog>

      <Dialog open={dialog === "upload"} onClose={() => setDialog(null)} title="Tải phiếu khám" submitLabel="Tải lên" onSubmit={upload}>
        <FilePicker
          label="Tệp"
          files={files}
          onChange={setFiles}
          accept="image/jpeg,image/png,image/webp,application/pdf"
          hint="JPG, PNG, WEBP hoặc PDF, tối đa 5 tệp, mỗi tệp không quá 5MB."
        />
      </Dialog>
    </>
  );
}
