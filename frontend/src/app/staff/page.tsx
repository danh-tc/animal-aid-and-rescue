"use client";

import { Ambulance, ClipboardCheck, FileText, HeartHandshake, Phone, Stethoscope } from "lucide-react";
import Link from "next/link";
import { Badge, StatusBadge } from "@/components/badge/Badge";
import { EmptyState } from "@/components/empty-state/EmptyState";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Panel } from "@/components/panel/Panel";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { StatCard } from "@/components/stat-card/StatCard";
import { useApi } from "@/hooks/useApi";
import { useAuth } from "@/lib/auth-context";
import { daysUntil, formatDate, formatDateTime, petName, todayIso } from "@/lib/format";
import { APPLICATION_STATUS, REPORT_STATUS } from "@/lib/labels";
import type { ApplicationView, Dashboard, PageResponse, ReportSummary, ScheduledFollowUp } from "@/types/api";
import "./dashboard.scss";

function dueLabel(date: string) {
  const d = daysUntil(date);
  if (d < 0) return <Badge tone="danger">Quá hạn {-d} ngày</Badge>;
  if (d === 0) return <Badge tone="warning">Hôm nay</Badge>;
  return <Badge tone="info">Còn {d} ngày</Badge>;
}

export default function StaffDashboardPage() {
  const { user } = useAuth();
  const stats = useApi<Dashboard>("/api/staff/dashboard");
  const reports = useApi<PageResponse<ReportSummary>>("/api/staff/rescue-reports", { status: "NEW", size: 5 });
  const apps = useApi<PageResponse<ApplicationView>>("/api/staff/applications", { status: "PENDING", size: 5 });
  const followUps = useApi<ScheduledFollowUp[]>("/api/staff/follow-ups", { dueBefore: todayIso(7) });
  const s = stats.data;

  return (
    <div className="dashboard">
      <PageHeader title={`Xin chào, ${user?.fullName ?? ""}`} description="Những việc cần xử lý hôm nay." />
      {stats.error && <Notice tone="error">{stats.error}</Notice>}
      {s && s.overdueUnassignedCases > 0 && (
        <Notice
          tone="warning"
          title={`${s.overdueUnassignedCases} ca cứu hộ quá hạn mà chưa được phân công`}
          action={<Link href="/staff/cases?status=NEW">Xem ca</Link>}
        >
          {user?.role === "MANAGER" ? "Hãy phân công người đi cứu." : "Chỉ Quản lý mới phân công được, hãy báo cho Quản lý."}
        </Notice>
      )}
      {s && s.recallPending > 0 && (
        <Notice tone="warning" title={`${s.recallPending} bàn giao đang chờ thu hồi`}>
          Liên hệ chủ nuôi để thu hồi thú. Xem ở tab Bàn giao trong hồ sơ thú. Quản lý xem danh sách đầy đủ ở màn hình Hậu kiểm.
        </Notice>
      )}

      <div className="dashboard__stats">
        <StatCard label="Báo cáo mới" value={s?.newReports ?? null} href="/staff/reports?status=NEW" icon={FileText} tone="accent" />
        <StatCard label="Ca đang mở" value={s?.openCases ?? null} href="/staff/cases" icon={Ambulance} tone="info" />
        <StatCard
          label="Đơn chờ duyệt"
          value={s?.pendingApplications ?? null}
          href="/staff/applications"
          icon={HeartHandshake}
          tone="primary"
        />
        <StatCard
          label="Hậu kiểm 7 ngày tới"
          value={s?.followUpsDueThisWeek ?? null}
          href="/staff/follow-ups"
          icon={ClipboardCheck}
          tone="warning"
        />
        <StatCard
          label="Yêu cầu thú y đang mở"
          value={s?.openVetRequests ?? null}
          href="/staff/vet-requests"
          icon={Stethoscope}
          tone="info"
        />
      </div>

      <div className="dashboard__columns">
        <Panel title="Báo cáo cứu hộ mới" actions={<Link href="/staff/reports?status=NEW">Xem tất cả</Link>}>
          {reports.loading && !reports.data && <Skeleton lines={3} />}
          {reports.data?.items.length === 0 && <EmptyState title="Không có báo cáo mới" icon={FileText} />}
          <ul className="dashboard__list">
            {reports.data?.items.map((r) => (
              <li key={r.id} className="dashboard__item">
                <Link href={`/staff/reports/${r.id}`} className="dashboard__link">
                  <span className="dashboard__item-title">
                    {r.code} <StatusBadge map={REPORT_STATUS} value={r.status} />
                  </span>
                  <span className="dashboard__item-text">{r.locationText}</span>
                  <span className="dashboard__item-meta">{formatDateTime(r.createdAt)}</span>
                </Link>
              </li>
            ))}
          </ul>
        </Panel>

        <Panel title="Đơn chờ duyệt" actions={<Link href="/staff/applications">Xem tất cả</Link>}>
          {apps.loading && !apps.data && <Skeleton lines={3} />}
          {apps.data?.items.length === 0 && <EmptyState title="Không có đơn chờ duyệt" icon={HeartHandshake} />}
          <ul className="dashboard__list">
            {apps.data?.items.map((a) => (
              <li key={a.id} className="dashboard__item">
                <Link href={`/staff/applications/${a.id}`} className="dashboard__link">
                  <span className="dashboard__item-title">
                    {petName(a.pet.name, a.pet.code)} <StatusBadge map={APPLICATION_STATUS} value={a.status} />
                  </span>
                  <span className="dashboard__item-text">{a.applicant.fullName}</span>
                  <span className="dashboard__item-meta">Nộp lúc {formatDateTime(a.submittedAt)}</span>
                </Link>
              </li>
            ))}
          </ul>
        </Panel>

        <Panel title="Hậu kiểm sắp đến hạn" actions={<Link href="/staff/follow-ups">Xem tất cả</Link>}>
          {followUps.loading && !followUps.data && <Skeleton lines={3} />}
          {followUps.data?.length === 0 && <EmptyState title="Không có lịch hậu kiểm trong 7 ngày" icon={ClipboardCheck} />}
          <ul className="dashboard__list">
            {followUps.data?.slice(0, 5).map((f) => (
              <li key={f.followUp.id} className="dashboard__item">
                <Link href={`/staff/follow-ups?focus=${f.followUp.id}`} className="dashboard__link">
                  <span className="dashboard__item-title">
                    {petName(f.petName, f.petCode)} · lần {f.followUp.sequenceNo} {dueLabel(f.followUp.dueDate)}
                  </span>
                  <span className="dashboard__item-text">
                    <Phone aria-hidden="true" /> {f.ownerName} {f.ownerPhone && `· ${f.ownerPhone}`}
                  </span>
                  <span className="dashboard__item-meta">Hạn {formatDate(f.followUp.dueDate)}</span>
                </Link>
              </li>
            ))}
          </ul>
        </Panel>
      </div>
    </div>
  );
}
