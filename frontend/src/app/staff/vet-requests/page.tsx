"use client";

import { Stethoscope } from "lucide-react";
import Link from "next/link";
import { StatusBadge } from "@/components/badge/Badge";
import { ListView } from "@/components/list-view/ListView";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Segmented } from "@/components/segmented/Segmented";
import { Toolbar } from "@/components/toolbar/Toolbar";
import { useApi } from "@/hooks/useApi";
import { usePageParam, useUrlState } from "@/hooks/useUrlState";
import { formatDateTime } from "@/lib/format";
import { CARE_MODE, PRIORITY, VET_STATUS } from "@/lib/labels";
import type { PageResponse, VetRequestStatus, VetRequestView } from "@/types/api";

type Filter = VetRequestStatus | "ALL";
const OPTIONS: { value: Filter; label: string }[] = [
  { value: "ALL", label: "Tất cả" },
  { value: "PENDING", label: "Chờ khám" },
  { value: "IN_TREATMENT", label: "Đang điều trị" },
  { value: "COMPLETED", label: "Hoàn tất" },
  { value: "CANCELLED", label: "Đã hủy" },
];

export default function VetRequestsPage() {
  const [status, setStatus] = useUrlState<Filter>("status", "ALL");
  const [page, setPage] = usePageParam();
  const { data, error, loading, reload } = useApi<PageResponse<VetRequestView>>("/api/staff/vet-requests", {
    status: status === "ALL" ? undefined : status,
    page,
  });

  return (
    <>
      <PageHeader title="Yêu cầu thú y" description="Tạo yêu cầu từ hồ sơ thú. Ghi kết quả khám, chi phí và phiếu khám tại đây." />
      <Toolbar>
        <Segmented label="Lọc theo trạng thái" options={OPTIONS} value={status} onChange={setStatus} />
      </Toolbar>
      <ListView
        caption="Danh sách yêu cầu thú y"
        data={data}
        loading={loading}
        error={error}
        onRetry={reload}
        onPage={setPage}
        emptyIcon={Stethoscope}
        emptyTitle="Không có yêu cầu thú y"
        rowKey={(v) => v.id}
        rowHref={(v) => `/staff/vet-requests/${v.id}`}
        columns={[
          { key: "id", header: "Yêu cầu", render: (v) => `#${v.id} · ${v.clinicName}` },
          { key: "pet", header: "Thú", render: (v) => <Link href={`/staff/pets/${v.petId}`} className="data-table__cell-link">Hồ sơ #{v.petId}</Link> },
          { key: "priority", header: "Ưu tiên", render: (v) => <StatusBadge map={PRIORITY} value={v.priority} /> },
          { key: "mode", header: "Hình thức", render: (v) => CARE_MODE[v.careMode], hideOnMobile: true },
          { key: "requested", header: "Tạo lúc", render: (v) => formatDateTime(v.requestedAt), hideOnMobile: true },
          { key: "status", header: "Trạng thái", render: (v) => <StatusBadge map={VET_STATUS} value={v.status} /> },
        ]}
      />
    </>
  );
}
