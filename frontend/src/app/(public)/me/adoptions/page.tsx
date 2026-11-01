"use client";

import { CalendarCheck, PawPrint } from "lucide-react";
import { Badge, StatusBadge } from "@/components/badge/Badge";
import { LinkButton } from "@/components/button/Button";
import { EmptyState } from "@/components/empty-state/EmptyState";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { Timeline } from "@/components/timeline/Timeline";
import { useApi } from "@/hooks/useApi";
import { daysUntil, formatDate, formatDateTime, petName } from "@/lib/format";
import { CONTACT_METHOD, FOLLOW_UP_RESULT, FOLLOW_UP_STATUS, HANDOVER_STATUS } from "@/lib/labels";
import type { FollowUpView, HandoverView } from "@/types/api";
import "./my-adoptions.scss";

function followUpTitle(f: FollowUpView) {
  return (
    <>
      Hậu kiểm lần {f.sequenceNo}
      {f.additional && " (bổ sung)"}
      {f.status === "DONE" && f.result ? (
        <StatusBadge map={FOLLOW_UP_RESULT} value={f.result} />
      ) : (
        <StatusBadge map={FOLLOW_UP_STATUS} value={f.status} />
      )}
    </>
  );
}

function followUpBody(f: FollowUpView) {
  if (f.status === "DONE")
    return `${f.contactMethod ? `${CONTACT_METHOD[f.contactMethod]}, ` : "Thực hiện "}${formatDateTime(f.performedAt)}${f.reason ? `. ${f.reason}` : ""}`;
  if (f.status === "CANCELLED") return "Đã hủy";
  const days = daysUntil(f.dueDate);
  if (days < 0) return `Đã quá hạn ${-days} ngày, nhân viên sẽ liên hệ sớm.`;
  if (days === 0) return "Đến hạn hôm nay.";
  return `Còn ${days} ngày. Nhân viên sẽ gọi điện hoặc đến thăm.`;
}

export default function MyAdoptionsPage() {
  const { data, error, loading } = useApi<HandoverView[]>("/api/adopter/adoptions");

  return (
    <>
      <PageHeader
        title="Thú đã nhận"
        description="Lịch hậu kiểm giúp trung tâm chắc chắn bé được chăm sóc tốt ở nhà mới."
        actions={
          <LinkButton href="/me/applications" variant="ghost">
            Đơn của tôi
          </LinkButton>
        }
      />
      {error && <Notice tone="error">{error}</Notice>}
      {loading && !data && <Skeleton card lines={5} />}
      {data && data.length === 0 && (
        <EmptyState
          icon={PawPrint}
          title="Bạn chưa nhận nuôi bé nào"
          description="Sau khi bàn giao, bé và lịch hậu kiểm sẽ hiện ở đây."
        />
      )}
      {data && data.length > 0 && (
        <div className="my-adoptions">
          {data.map((h) => (
            <article key={h.id} className="my-adoptions__card">
              <header className="my-adoptions__head">
                <span className="my-adoptions__icon" aria-hidden="true">
                  <PawPrint />
                </span>
                <div className="my-adoptions__title-wrap">
                  <h2 className="my-adoptions__title">{petName(h.petName, h.petCode)}</h2>
                  <p className="my-adoptions__meta">
                    Mã {h.petCode} · Nhận ngày {formatDate(h.handedOverAt)}
                  </p>
                </div>
                <StatusBadge map={HANDOVER_STATUS} value={h.status} />
              </header>
              <h3 className="my-adoptions__subtitle">
                <CalendarCheck aria-hidden="true" />
                Lịch hậu kiểm
              </h3>
              {h.followUps.length === 0 ? (
                <p className="my-adoptions__meta">Chưa có lịch.</p>
              ) : (
                <Timeline
                  entries={h.followUps.map((f) => ({
                    key: f.id,
                    title: followUpTitle(f),
                    time: formatDate(f.dueDate),
                    body: followUpBody(f),
                  }))}
                />
              )}
              {h.status === "RECALL_PENDING" && (
                <Notice tone="warning" title="Trung tâm cần trao đổi với bạn">
                  Kết quả hậu kiểm chưa đạt. Nhân viên sẽ liên hệ để thống nhất cách chăm sóc bé.
                </Notice>
              )}
              {h.status === "COMPLETED" && <Badge tone="success">Đã hoàn tất hậu kiểm, cảm ơn bạn!</Badge>}
            </article>
          ))}
        </div>
      )}
    </>
  );
}
