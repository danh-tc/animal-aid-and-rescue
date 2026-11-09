"use client";

import { FileText } from "lucide-react";
import { StatusBadge } from "@/components/badge/Badge";
import { ListView } from "@/components/list-view/ListView";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Segmented } from "@/components/segmented/Segmented";
import { Toolbar } from "@/components/toolbar/Toolbar";
import { useApi } from "@/hooks/useApi";
import { usePageParam, useUrlState } from "@/hooks/useUrlState";
import { formatDateTime } from "@/lib/format";
import { REPORT_STATUS } from "@/lib/labels";
import type { PageResponse, ReportStatus, ReportSummary } from "@/types/api";

type Filter = ReportStatus | "ALL";
const OPTIONS: { value: Filter; label: string }[] = [
  { value: "NEW", label: "Mới" },
  { value: "PROCESSED", label: "Đã xử lý" },
  { value: "INVALID", label: "Không hợp lệ" },
  { value: "ALL", label: "Tất cả" },
];

export default function ReportsPage() {
  const [status, setStatus] = useUrlState<Filter>("status", "NEW");
  const [page, setPage] = usePageParam();
  const { data, error, loading, reload } = useApi<PageResponse<ReportSummary>>("/api/staff/rescue-reports", {
    status: status === "ALL" ? undefined : status,
    page,
  });

  return (
    <>
      <PageHeader title="Báo cáo cứu hộ" description="Báo cáo do người dân gửi. Tạo ca cứu hộ hoặc đánh dấu không hợp lệ." />
      <Toolbar>
        <Segmented label="Lọc theo trạng thái" options={OPTIONS} value={status} onChange={setStatus} />
      </Toolbar>
      <ListView
        caption="Danh sách báo cáo cứu hộ"
        data={data}
        loading={loading}
        error={error}
        onRetry={reload}
        onPage={setPage}
        emptyIcon={FileText}
        emptyTitle="Không có báo cáo nào"
        emptyDescription={status === "NEW" ? "Mọi báo cáo mới đã được xử lý." : undefined}
        rowKey={(r) => r.id}
        rowHref={(r) => `/staff/reports/${r.id}`}
        columns={[
          { key: "code", header: "Mã", render: (r) => r.code },
          { key: "location", header: "Vị trí", render: (r) => r.locationText },
          { key: "reporter", header: "Người báo", render: (r) => `${r.reporterName} · ${r.reporterPhone}`, hideOnMobile: true },
          { key: "created", header: "Thời gian", render: (r) => formatDateTime(r.createdAt) },
          { key: "status", header: "Trạng thái", render: (r) => <StatusBadge map={REPORT_STATUS} value={r.status} /> },
        ]}
      />
    </>
  );
}
