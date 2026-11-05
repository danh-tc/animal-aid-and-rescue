"use client";

import { Ban, CalendarClock, Check, Mail, PackageCheck, Phone, X } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { Badge, StatusBadge } from "@/components/badge/Badge";
import { Button, LinkButton } from "@/components/button/Button";
import { CostSummary } from "@/components/cost-summary/CostSummary";
import { DetailList } from "@/components/detail-list/DetailList";
import { Dialog } from "@/components/dialog/Dialog";
import { Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Panel } from "@/components/panel/Panel";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { ApiRequestError } from "@/lib/api";
import { formatDate, formatDateTime, petName, todayIso } from "@/lib/format";
import { APPLICATION_STATUS, HOUSING, PET_STATUS } from "@/lib/labels";
import type { ApplicationDetail, HandoverView } from "@/types/api";
import "../../detail-page.scss";

/** S07: Xét duyệt đơn. S08: Bàn giao khi đơn đã duyệt. */
export default function ApplicationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data, error, loading, reload } = useApi<ApplicationDetail>(`/api/staff/applications/${id}`);
  const mutate = useMutation();
  const toast = useToast();
  const [dialog, setDialog] = useState<"approve" | "reject" | "handover" | "schedule" | "cancel" | null>(null);
  const [handoverDate, setHandoverDate] = useState(todayIso(2));
  const [note, setNote] = useState("");
  const [donation, setDonation] = useState("0");
  const [handover, setHandover] = useState<HandoverView | null>(null);

  if (loading && !data) return <Skeleton card lines={8} />;
  if (!data) return <Notice tone="error">{error}</Notice>;
  const a = data.application;
  const name = petName(a.pet.name, a.pet.code);

  function openDialog(kind: "approve" | "reject" | "handover" | "schedule" | "cancel") {
    setNote("");
    setHandoverDate(a.handoverDate ?? todayIso(2));
    setDialog(kind);
  }

  async function approve() {
    try {
      if (!handoverDate || handoverDate < todayIso()) throw new Error("Chọn ngày hẹn bàn giao từ hôm nay trở đi.");
      await mutate(`/api/staff/applications/${id}/approve`, { body: { note: note.trim() || null, handoverDate } });
    } catch (e) {
      if (e instanceof ApiRequestError && e.code === "PET_ALREADY_APPROVED") {
        reload();
        throw new Error("Bé đã có đơn khác được duyệt trước đó. Trang đã cập nhật lại trạng thái.");
      }
      throw e;
    }
    toast.success(`Đã duyệt đơn. ${name} chuyển sang Chờ bàn giao, các đơn chờ khác đã tự đóng.`);
    setDialog(null);
    reload();
  }

  async function schedule() {
    if (!handoverDate || handoverDate < todayIso()) throw new Error("Chọn ngày hẹn bàn giao từ hôm nay trở đi.");
    await mutate(`/api/staff/applications/${id}/handover-date`, { body: { handoverDate } });
    toast.success(`Đã hẹn bàn giao ngày ${formatDate(handoverDate)}.`);
    setDialog(null);
    reload();
  }

  /** QĐ-11: người nhận không đến bàn giao theo hẹn, nhân viên hủy đơn và ghi lý do, thú về Sẵn sàng. */
  async function cancelNoShow() {
    if (!note.trim()) throw new Error("Ghi lý do hủy, ví dụ không đến theo hẹn và không liên lạc được.");
    await mutate(`/api/staff/applications/${id}/cancel`, { body: { reason: note.trim() } });
    toast.success(`Đã hủy đơn. ${name} trở lại Sẵn sàng nhận nuôi.`);
    setDialog(null);
    reload();
  }

  async function reject() {
    await mutate(`/api/staff/applications/${id}/reject`, { body: { note: note.trim() } });
    toast.success("Đã từ chối đơn.");
    setDialog(null);
    reload();
  }

  async function doHandover() {
    const amount = Number(donation || "0");
    if (!(amount >= 0) || !Number.isInteger(amount)) throw new Error("Khoản ủng hộ là số nguyên VND, không âm.");
    const res = await mutate<HandoverView>(`/api/staff/applications/${id}/handover`, {
      body: { note: note.trim() || null, donationAmount: amount },
    });
    setHandover(res);
    toast.success(`Đã bàn giao ${name}. Hệ thống tạo ${res.followUps.length} lịch hậu kiểm.`);
    setDialog(null);
    reload();
  }

  return (
    <>
      <PageHeader
        back={{ href: "/staff/applications", label: "Đơn nhận nuôi" }}
        title={`Đơn #${a.id} · ${name}`}
        meta={
          <>
            <StatusBadge map={APPLICATION_STATUS} value={a.status} />
            <span>Nộp lúc {formatDateTime(a.submittedAt)}</span>
          </>
        }
        actions={
          <>
            {a.status === "PENDING" && (
              <>
                <Button variant="ghost" icon={<X aria-hidden="true" />} onClick={() => openDialog("reject")}>
                  Từ chối
                </Button>
                <Button
                  icon={<Check aria-hidden="true" />}
                  onClick={() => openDialog("approve")}
                  disabled={data.petStatus !== "AVAILABLE"}
                >
                  Duyệt đơn
                </Button>
              </>
            )}
            {a.status === "APPROVED" && (
              <>
                <Button variant="ghost" icon={<Ban aria-hidden="true" />} onClick={() => openDialog("cancel")}>
                  Hủy do không đến
                </Button>
                <Button variant="ghost" icon={<CalendarClock aria-hidden="true" />} onClick={() => openDialog("schedule")}>
                  {a.handoverDate ? "Đổi ngày hẹn" : "Hẹn ngày bàn giao"}
                </Button>
                <Button icon={<PackageCheck aria-hidden="true" />} onClick={() => openDialog("handover")}>
                  Bàn giao
                </Button>
              </>
            )}
          </>
        }
      />

      <div className="detail-page">
        <div className="detail-page__main">
          {data.applicantHistory.filter((f) => f.kind === "RECALLED").map((f) => (
            <Notice key={`r-${f.petId}`} tone="error" title="Người nộp từng bị thu hồi thú">
              {petName(f.petName, f.petCode)} ({f.petCode}), bàn giao {formatDate(f.handedOverAt)}: {f.note}.{" "}
              <Link href={`/staff/pets/${f.petId}?tab=handovers`}>Xem lịch sử</Link>
            </Notice>
          ))}
          {data.applicantHistory.filter((f) => f.kind === "RETURNED").map((f) => (
            <Notice key={`t-${f.petId}`} tone="warning" title="Người nộp từng trả lại thú">
              {petName(f.petName, f.petCode)} ({f.petCode}), bàn giao {formatDate(f.handedOverAt)}. {f.note}.{" "}
              <Link href={`/staff/pets/${f.petId}?tab=handovers`}>Xem lịch sử</Link>
            </Notice>
          ))}
          {a.status === "APPROVED" && (
            <Notice tone={a.handoverDate ? "info" : "warning"} title={a.handoverDate ? `Hẹn bàn giao ngày ${formatDate(a.handoverDate)}` : "Chưa hẹn ngày bàn giao"}>
              {a.handoverDate ? "Người nhận nuôi thấy ngày hẹn trong mục Đơn của tôi." : "Bấm Hẹn ngày bàn giao để báo cho người nhận nuôi."}
            </Notice>
          )}
          {a.status === "PENDING" && data.petStatus !== "AVAILABLE" && (
            <Notice tone="warning" title="Chưa duyệt được">
              Bé đang ở trạng thái {PET_STATUS[data.petStatus].label.toLowerCase()}, chỉ duyệt được khi bé Sẵn sàng nhận nuôi.
            </Notice>
          )}
          {handover && (
            <Notice
              tone="success"
              title="Đã bàn giao"
              action={<LinkButton href={`/staff/pets/${a.pet.id}?tab=handovers`} size="sm" variant="secondary">Xem lịch hậu kiểm</LinkButton>}
            >
              Lịch hậu kiểm: {handover.followUps.map((f) => `lần ${f.sequenceNo} ngày ${f.dueDate}`).join(", ")}.
            </Notice>
          )}
          <Panel title="Người nộp đơn">
            <DetailList
              items={[
                { label: "Họ tên", value: a.applicant.fullName },
                {
                  label: "Liên hệ",
                  value: (
                    <span className="stack stack--sm">
                      {a.applicant.phone && (
                        <a href={`tel:${a.applicant.phone}`} className="detail-page__inline-link">
                          <Phone aria-hidden="true" /> {a.applicant.phone}
                        </a>
                      )}
                      <a href={`mailto:${a.applicant.email}`} className="detail-page__inline-link">
                        <Mail aria-hidden="true" /> {a.applicant.email}
                      </a>
                    </span>
                  ),
                },
                { label: "Địa chỉ", value: a.applicant.address, wide: true },
              ]}
            />
          </Panel>
          <Panel title="Nội dung đơn">
            <DetailList
              items={[
                { label: "Nhà ở", value: HOUSING[a.housingType] },
                { label: "Kinh nghiệm", value: a.hasPetExperience ? "Đã từng nuôi chó mèo" : "Chưa từng nuôi" },
                { label: "Lý do nhận nuôi", value: a.motivation, wide: true },
                ...(a.reviewedAt
                  ? [{ label: "Xét duyệt", value: `${formatDateTime(a.reviewedAt)}${a.reviewNote ? `\n${a.reviewNote}` : ""}`, wide: true }]
                  : []),
                ...(a.cancelReason ? [{ label: "Lý do hủy", value: a.cancelReason, wide: true }] : []),
              ]}
            />
          </Panel>
        </div>

        <aside className="detail-page__side">
          <Panel title="Thú">
            <div className="stack">
              <p>
                <strong>{name}</strong> · {a.pet.code} <StatusBadge map={PET_STATUS} value={data.petStatus} />
              </p>
              <LinkButton href={`/staff/pets/${a.pet.id}`} variant="secondary" block>
                Mở hồ sơ thú
              </LinkButton>
            </div>
          </Panel>
          <Panel
            title="Các đơn khác cho bé này"
            description={data.otherApplications.length === 0 ? "Không có đơn nào khác." : "Dùng để so sánh trước khi duyệt."}
          >
            <ul className="detail-page__links">
              {data.otherApplications.map((o) => (
                <li key={o.id}>
                  <Link href={`/staff/applications/${o.id}`} className="detail-page__link">
                    <span>
                      Đơn #{o.id} · {o.applicant.fullName}
                    </span>
                    <StatusBadge map={APPLICATION_STATUS} value={o.status} />
                    <span className="detail-page__link-meta">
                      {HOUSING[o.housingType]} · {o.hasPetExperience ? "Có kinh nghiệm" : "Chưa có kinh nghiệm"} · {formatDateTime(o.submittedAt)}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </Panel>
        </aside>
      </div>

      <Dialog
        open={dialog === "approve"}
        onClose={() => setDialog(null)}
        title={`Duyệt đơn nhận nuôi ${name}?`}
        description="Bé chuyển sang Chờ bàn giao. Các đơn đang chờ khác cho bé sẽ tự đóng."
        submitLabel="Duyệt đơn"
        onSubmit={approve}
      >
        <Field label="Ngày hẹn bàn giao" required hint="Người nhận nuôi thấy ngày này trong mục Đơn của tôi, đổi được sau.">
          <input type="date" min={todayIso()} value={handoverDate} onChange={(e) => setHandoverDate(e.target.value)} required />
        </Field>
        <Field label="Ghi chú" hint="Không bắt buộc, người nộp đơn sẽ thấy ghi chú này.">
          <textarea rows={3} maxLength={2000} value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
      </Dialog>

      <Dialog
        open={dialog === "schedule"}
        onClose={() => setDialog(null)}
        title="Hẹn ngày bàn giao"
        submitLabel="Lưu ngày hẹn"
        onSubmit={schedule}
      >
        <Field label="Ngày hẹn bàn giao" required>
          <input type="date" min={todayIso()} value={handoverDate} onChange={(e) => setHandoverDate(e.target.value)} required />
        </Field>
      </Dialog>

      <Dialog
        open={dialog === "cancel"}
        onClose={() => setDialog(null)}
        title="Hủy đơn đã duyệt?"
        description={`Dùng khi người nhận không đến bàn giao theo hẹn và không liên lạc được. ${name} trở lại Sẵn sàng, các đơn đã đóng không mở lại.`}
        submitLabel="Hủy đơn"
        submitVariant="danger"
        onSubmit={cancelNoShow}
      >
        <Field label="Lý do" required hint="Ví dụ: hẹn ngày 12/11, gọi 3 lần không nghe máy.">
          <textarea rows={3} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} required />
        </Field>
      </Dialog>

      <Dialog
        open={dialog === "reject"}
        onClose={() => setDialog(null)}
        title="Từ chối đơn?"
        submitLabel="Từ chối"
        submitVariant="danger"
        onSubmit={reject}
      >
        <Field label="Lý do" required hint="Người nộp đơn sẽ thấy lý do này, nên viết lịch sự và cụ thể.">
          <textarea rows={3} maxLength={2000} value={note} onChange={(e) => setNote(e.target.value)} required />
        </Field>
      </Dialog>

      <Dialog
        open={dialog === "handover"}
        onClose={() => setDialog(null)}
        title={`Bàn giao ${name}`}
        description="Cho người nhận xem 3 con số chi phí của bé, ghi khoản ủng hộ nếu có. Hệ thống tự tạo 2 lịch hậu kiểm."
        submitLabel="Xác nhận bàn giao"
        onSubmit={doHandover}
      >
        <CostSummary petId={a.pet.id} />
        <Field label="Khoản ủng hộ (VND)" hint="Tùy tâm, không hoàn lại khi trả thú. Hệ thống chỉ ghi nhận số tiền.">
          <input type="number" min={0} step={10000} inputMode="numeric" value={donation} onChange={(e) => setDonation(e.target.value)} />
        </Field>
        <Field label="Ghi chú">
          <textarea rows={3} maxLength={2000} value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
      </Dialog>
    </>
  );
}
