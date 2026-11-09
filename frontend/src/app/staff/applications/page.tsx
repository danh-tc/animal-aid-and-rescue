"use client";

import { HeartHandshake } from "lucide-react";
import { StatusBadge } from "@/components/badge/Badge";
import { ListView } from "@/components/list-view/ListView";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Segmented } from "@/components/segmented/Segmented";
import { Toolbar } from "@/components/toolbar/Toolbar";
import { useApi } from "@/hooks/useApi";
import { usePageParam, useUrlState } from "@/hooks/useUrlState";
import { formatDateTime, petName } from "@/lib/format";
import { APPLICATION_STATUS, HOUSING } from "@/lib/labels";
import type { ApplicationStatus, ApplicationView, PageResponse } from "@/types/api";

type Filter = ApplicationStatus | "ALL";
const OPTIONS: { value: Filter; label: string }[] = [
  { value: "PENDING", label: "Chờ xét duyệt" },
  { value: "APPROVED", label: "Đã duyệt, chờ bàn giao" },
  { value: "HANDED_OVER", label: "Đã bàn giao" },
  { value: "REJECTED", label: "Từ chối" },
  { value: "CANCELLED", label: "Đã hủy" },
  { value: "ALL", label: "Tất cả" },
];

export default function ApplicationsPage() {
  const [status, setStatus] = useUrlState<Filter>("status", "PENDING");
  const [page, setPage] = usePageParam();
  const { data, error, loading, reload } = useApi<PageResponse<ApplicationView>>("/api/staff/applications", {
    status: status === "ALL" ? undefined : status,
    page,
  });

  return (
    <>
      <PageHeader title="Đơn nhận nuôi" description="Xét duyệt đơn, so sánh các đơn cùng một bé và bàn giao sau khi duyệt." />
      <Toolbar>
        <Segmented label="Lọc theo trạng thái" options={OPTIONS} value={status} onChange={setStatus} />
      </Toolbar>
      <ListView
        caption="Danh sách đơn nhận nuôi"
        data={data}
        loading={loading}
        error={error}
        onRetry={reload}
        onPage={setPage}
        emptyIcon={HeartHandshake}
        emptyTitle="Không có đơn nào"
        rowKey={(a) => a.id}
        rowHref={(a) => `/staff/applications/${a.id}`}
        columns={[
          { key: "pet", header: "Thú", render: (a) => `${petName(a.pet.name, a.pet.code)} · ${a.pet.code}` },
          { key: "applicant", header: "Người nộp", render: (a) => a.applicant.fullName },
          { key: "housing", header: "Nhà ở", render: (a) => HOUSING[a.housingType], hideOnMobile: true },
          { key: "submitted", header: "Nộp lúc", render: (a) => formatDateTime(a.submittedAt), hideOnMobile: true },
          { key: "status", header: "Trạng thái", render: (a) => <StatusBadge map={APPLICATION_STATUS} value={a.status} /> },
        ]}
      />
    </>
  );
}
