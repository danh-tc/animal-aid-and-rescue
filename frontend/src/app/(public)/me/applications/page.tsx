"use client";

import { ClipboardList, HeartHandshake } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { Button, LinkButton } from "@/components/button/Button";
import { StatusBadge } from "@/components/badge/Badge";
import { Dialog } from "@/components/dialog/Dialog";
import { EmptyState } from "@/components/empty-state/EmptyState";
import { Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { formatDate, formatDateTime, petName } from "@/lib/format";
import { APPLICATION_STATUS, HOUSING } from "@/lib/labels";
import type { ApplicationView } from "@/types/api";
import "./my-applications.scss";

export default function MyApplicationsPage() {
  const { data, error, loading, reload } = useApi<ApplicationView[]>("/api/adopter/applications");
  const mutate = useMutation();
  const toast = useToast();
  const [cancelling, setCancelling] = useState<ApplicationView | null>(null);
  const [reason, setReason] = useState("");

  async function confirmCancel() {
    if (!cancelling) return;
    await mutate(`/api/adopter/applications/${cancelling.id}/cancel`, { body: { reason: reason.trim() || null } });
    toast.success("Đã hủy đơn.");
    setCancelling(null);
    setReason("");
    reload();
  }

  return (
    <>
      <PageHeader
        title="Đơn của tôi"
        description="Theo dõi trạng thái các đơn nhận nuôi. Nhân viên sẽ liên hệ qua số điện thoại của bạn."
        actions={
          <LinkButton href="/me/adoptions" variant="ghost">
            Thú đã nhận
          </LinkButton>
        }
      />
      {error && <Notice tone="error">{error}</Notice>}
      {loading && !data && <Skeleton card lines={4} />}
      {data && data.length === 0 && (
        <EmptyState
          icon={ClipboardList}
          title="Bạn chưa nộp đơn nào"
          description="Chọn một bé bạn yêu thích rồi bấm Nộp đơn nhận nuôi."
          action={<LinkButton href="/" icon={<HeartHandshake aria-hidden="true" />}>Xem các bé</LinkButton>}
        />
      )}
      {data && data.length > 0 && (
        <ul className="my-applications">
          {data.map((a) => (
            <li key={a.id} className="my-applications__item">
              <div className="my-applications__head">
                <div>
                  <h2 className="my-applications__pet">
                    {a.status === "PENDING" ? (
                      <Link href={`/pets/${a.pet.id}`}>{petName(a.pet.name, a.pet.code)}</Link>
                    ) : (
                      petName(a.pet.name, a.pet.code)
                    )}
                  </h2>
                  <p className="my-applications__meta">
                    Đơn số {a.id} · Mã thú {a.pet.code} · Nộp lúc {formatDateTime(a.submittedAt)}
                  </p>
                </div>
                <StatusBadge map={APPLICATION_STATUS} value={a.status} />
              </div>
              <p className="my-applications__detail">
                Nhà ở: {HOUSING[a.housingType]} · {a.hasPetExperience ? "Đã từng nuôi" : "Chưa từng nuôi"}
                {a.sterilizationPledge && " · Đã cam kết triệt sản"}
              </p>
              {a.status === "APPROVED" && (
                <Notice tone="success" title="Đơn đã được duyệt">
                  {a.handoverDate
                    ? `Hẹn bàn giao ngày ${formatDate(a.handoverDate)}. Nhân viên sẽ liên hệ xác nhận trước khi đón bé.`
                    : "Nhân viên sẽ liên hệ để hẹn ngày bàn giao."}{" "}
                  {a.reviewNote}
                </Notice>
              )}
              {a.status === "REJECTED" && (
                <Notice tone="warning" title="Đơn chưa phù hợp">
                  {a.reviewNote}
                </Notice>
              )}
              {a.status === "CANCELLED" && a.cancelReason && (
                <p className="my-applications__note">Lý do hủy: {a.cancelReason}</p>
              )}
              {(a.status === "PENDING" || a.status === "APPROVED") && (
                <div className="my-applications__actions">
                  <Button variant="ghost" size="sm" onClick={() => setCancelling(a)}>
                    Hủy đơn
                  </Button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      <Dialog
        open={cancelling !== null}
        onClose={() => setCancelling(null)}
        title="Hủy đơn nhận nuôi?"
        description={
          cancelling?.status === "APPROVED"
            ? "Đơn đã được duyệt. Hủy đơn thì bé sẽ mở lại cho người khác nhận nuôi."
            : "Bạn có thể nộp lại đơn sau nếu bé vẫn chờ nhận nuôi."
        }
        submitLabel="Hủy đơn"
        submitVariant="danger"
        onSubmit={confirmCancel}
      >
        <Field label="Lý do" hint="Không bắt buộc, giúp trung tâm cải thiện quy trình.">
          <textarea rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} />
        </Field>
      </Dialog>
    </>
  );
}
