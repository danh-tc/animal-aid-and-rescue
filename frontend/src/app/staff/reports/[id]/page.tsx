"use client";

import { Ambulance, Ban, MapPin, Phone } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { AttachmentList } from "@/components/attachment-list/AttachmentList";
import { StatusBadge } from "@/components/badge/Badge";
import { Button } from "@/components/button/Button";
import { DetailList } from "@/components/detail-list/DetailList";
import { Dialog } from "@/components/dialog/Dialog";
import { Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Panel } from "@/components/panel/Panel";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { useStaffMembers } from "@/hooks/useStaffMembers";
import { formatDateTime } from "@/lib/format";
import { CASE_STATUS, REPORT_STATUS } from "@/lib/labels";
import type { CaseSummary, ReportDetail } from "@/types/api";
import "../../detail-page.scss";

/** S02: Chi tiết báo cáo, tạo ca, đánh dấu không hợp lệ */
export default function ReportDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: r, error, loading, reload } = useApi<ReportDetail>(`/api/staff/rescue-reports/${id}`);
  const { members, nameOf } = useStaffMembers();
  const mutate = useMutation();
  const toast = useToast();
  const [dialog, setDialog] = useState<"cases" | "invalid" | null>(null);
  const [count, setCount] = useState("1");
  const [assignee, setAssignee] = useState("");
  const [reason, setReason] = useState("");

  if (loading && !r) return <Skeleton card lines={6} />;
  if (!r) return <Notice tone="error">{error}</Notice>;

  const isNew = r.status === "NEW";
  const mapUrl =
    r.latitude !== null && r.longitude !== null
      ? `https://www.google.com/maps/search/?api=1&query=${r.latitude},${r.longitude}`
      : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(r.locationText)}`;

  async function createCases() {
    const n = Number.parseInt(count, 10);
    if (!(n >= 1 && n <= 20)) throw new Error("Số thú từ 1 đến 20");
    const created = await mutate<CaseSummary[]>(`/api/staff/rescue-reports/${id}/cases`, {
      body: { numberOfAnimals: n, assignedStaffId: assignee ? Number(assignee) : null },
    });
    toast.success(`Đã tạo ${created.length} ca cứu hộ.`);
    setDialog(null);
    reload();
  }

  async function markInvalid() {
    await mutate(`/api/staff/rescue-reports/${id}/invalid`, { body: { reason: reason.trim() } });
    toast.success("Đã đánh dấu báo cáo không hợp lệ.");
    setDialog(null);
    reload();
  }

  return (
    <>
      <PageHeader
        back={{ href: "/staff/reports", label: "Báo cáo cứu hộ" }}
        title={`Báo cáo ${r.code}`}
        meta={
          <>
            <StatusBadge map={REPORT_STATUS} value={r.status} />
            <span>Gửi lúc {formatDateTime(r.createdAt)}</span>
          </>
        }
        actions={
          isNew && (
            <>
              <Button variant="ghost" icon={<Ban aria-hidden="true" />} onClick={() => setDialog("invalid")}>
                Không hợp lệ
              </Button>
              <Button icon={<Ambulance aria-hidden="true" />} onClick={() => setDialog("cases")}>
                Tạo ca cứu hộ
              </Button>
            </>
          )
        }
      />

      <div className="detail-page">
        <div className="detail-page__main">
          <Panel title="Thông tin báo cáo">
            <DetailList
              items={[
                {
                  label: "Người báo",
                  value: (
                    <>
                      {r.reporterName} ·{" "}
                      <a href={`tel:${r.reporterPhone}`} className="detail-page__inline-link">
                        <Phone aria-hidden="true" />
                        {r.reporterPhone}
                      </a>
                    </>
                  ),
                },
                {
                  label: "Vị trí",
                  value: (
                    <a href={mapUrl} target="_blank" rel="noreferrer" className="detail-page__inline-link">
                      <MapPin aria-hidden="true" />
                      {r.locationText}
                    </a>
                  ),
                },
                { label: "Mô tả", value: r.description, wide: true },
                ...(r.handledAt
                  ? [{ label: "Xử lý", value: `${nameOf(r.handledBy)} · ${formatDateTime(r.handledAt)}` }]
                  : []),
              ]}
            />
          </Panel>
          <Panel title="Ảnh hiện trường">
            <AttachmentList items={r.photos} />
          </Panel>
        </div>

        <aside className="detail-page__side">
          <Panel title="Ca cứu hộ" description={r.cases.length === 0 ? "Chưa tạo ca nào." : undefined}>
            <ul className="detail-page__links">
              {r.cases.map((c) => (
                <li key={c.id}>
                  <Link href={`/staff/cases/${c.id}`} className="detail-page__link">
                    <span>Ca #{c.id}</span>
                    <StatusBadge map={CASE_STATUS} value={c.status} />
                    <span className="detail-page__link-meta">Phụ trách: {nameOf(c.assignedStaffId)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </Panel>
        </aside>
      </div>

      <Dialog
        open={dialog === "cases"}
        onClose={() => setDialog(null)}
        title="Tạo ca cứu hộ"
        description="Mỗi con vật một ca. Báo cáo có nhiều con thì tạo nhiều ca cùng lúc."
        submitLabel="Tạo ca"
        onSubmit={createCases}
      >
        <Field label="Số con vật" required hint="Từ 1 đến 20.">
          <input type="number" min={1} max={20} inputMode="numeric" value={count} onChange={(e) => setCount(e.target.value)} />
        </Field>
        <Field label="Phân công cho" hint="Có thể để trống và phân công sau.">
          <select value={assignee} onChange={(e) => setAssignee(e.target.value)}>
            <option value="">Chưa phân công</option>
            {members.map((m) => (
              <option key={m.id} value={m.id}>
                {m.fullName}
              </option>
            ))}
          </select>
        </Field>
      </Dialog>

      <Dialog
        open={dialog === "invalid"}
        onClose={() => setDialog(null)}
        title="Đánh dấu không hợp lệ?"
        description="Báo cáo sẽ đóng và không tạo ca cứu hộ."
        submitLabel="Xác nhận"
        submitVariant="danger"
        onSubmit={markInvalid}
      >
        <Field label="Lý do" required>
          <textarea rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} required />
        </Field>
      </Dialog>
    </>
  );
}
