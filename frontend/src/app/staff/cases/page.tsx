"use client";

import { Ambulance } from "lucide-react";
import { StatusBadge } from "@/components/badge/Badge";
import { ListView } from "@/components/list-view/ListView";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Segmented } from "@/components/segmented/Segmented";
import { Toolbar } from "@/components/toolbar/Toolbar";
import { useApi } from "@/hooks/useApi";
import { useStaffMembers } from "@/hooks/useStaffMembers";
import { usePageParam, useUrlState } from "@/hooks/useUrlState";
import { formatDateTime } from "@/lib/format";
import { CASE_STATUS } from "@/lib/labels";
import type { CaseStatus, CaseSummary, PageResponse } from "@/types/api";

type Filter = CaseStatus | "ALL";
const OPTIONS: { value: Filter; label: string }[] = [
  { value: "ALL", label: "Tất cả" },
  { value: "NEW", label: "Chưa phân công" },
  { value: "IN_PROGRESS", label: "Đang xử lý" },
  { value: "SUCCEEDED", label: "Thành công" },
  { value: "FAILED", label: "Thất bại" },
];

/** S03: Danh sách ca cứu hộ */
export default function CasesPage() {
  const [status, setStatus] = useUrlState<Filter>("status", "ALL");
  const [page, setPage] = usePageParam();
  const { nameOf } = useStaffMembers();
  const { data, error, loading, reload } = useApi<PageResponse<CaseSummary>>("/api/staff/rescue-cases", {
    status: status === "ALL" ? undefined : status,
    page,
  });

  return (
    <>
      <PageHeader
        title="Ca cứu hộ"
        description="Quản lý phân công người đi cứu. Nhân viên phụ trách ghi kết quả và tạo hồ sơ thú khi cứu thành công."
      />
      <Toolbar>
        <Segmented label="Lọc theo trạng thái" options={OPTIONS} value={status} onChange={setStatus} />
      </Toolbar>
      <ListView
        caption="Danh sách ca cứu hộ"
        data={data}
        loading={loading}
        error={error}
        onRetry={reload}
        onPage={setPage}
        emptyIcon={Ambulance}
        emptyTitle="Không có ca cứu hộ nào"
        rowKey={(c) => c.id}
        rowHref={(c) => `/staff/cases/${c.id}`}
        columns={[
          { key: "id", header: "Ca", render: (c) => `#${c.id}` },
          { key: "location", header: "Vị trí", render: (c) => c.locationText },
          { key: "staff", header: "Phụ trách", render: (c) => nameOf(c.assignedStaffId) },
          { key: "created", header: "Tạo lúc", render: (c) => formatDateTime(c.createdAt), hideOnMobile: true },
          { key: "status", header: "Trạng thái", render: (c) => <StatusBadge map={CASE_STATUS} value={c.status} /> },
        ]}
      />
    </>
  );
}
