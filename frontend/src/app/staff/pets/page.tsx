"use client";

import { PawPrint, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { StatusBadge } from "@/components/badge/Badge";
import { ListView } from "@/components/list-view/ListView";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Segmented } from "@/components/segmented/Segmented";
import { Toolbar } from "@/components/toolbar/Toolbar";
import { useApi } from "@/hooks/useApi";
import { usePageParam, useUrlState } from "@/hooks/useUrlState";
import { formatDate, petName } from "@/lib/format";
import { AREA, PET_STATUS, SPECIES } from "@/lib/labels";
import type { PageResponse, PetStatus, PetSummary, Species } from "@/types/api";
import "./pet-search.scss";

type StatusFilter = PetStatus | "ALL";
const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "Tất cả" },
  { value: "IN_CARE", label: "Đang chăm sóc" },
  { value: "IN_TREATMENT", label: "Đang điều trị" },
  { value: "AVAILABLE", label: "Sẵn sàng" },
  { value: "PENDING_HANDOVER", label: "Chờ bàn giao" },
  { value: "ADOPTED", label: "Đã có chủ" },
  { value: "LOST", label: "Thất lạc" },
  { value: "DECEASED", label: "Đã mất" },
  { value: "TRANSFERRED", label: "Đã chuyển đi" },
];

/** S04: Danh sách thú, lọc theo trạng thái, loài, tìm theo tên hoặc mã */
export default function PetsPage() {
  const [status, setStatus] = useUrlState<StatusFilter>("status", "ALL");
  const [species, setSpecies] = useUrlState<Species | "">("species", "");
  const [q, setQ] = useUrlState<string>("q", "");
  const [page, setPage] = usePageParam();
  const [text, setText] = useState(q);

  // Gõ xong 350ms mới tìm để không gọi API mỗi phím
  useEffect(() => {
    const t = window.setTimeout(() => text.trim() !== q && setQ(text.trim()), 350);
    return () => window.clearTimeout(t);
  }, [text, q, setQ]);

  const { data, error, loading, reload } = useApi<PageResponse<PetSummary>>("/api/staff/pets", {
    status: status === "ALL" ? undefined : status,
    species: species || undefined,
    q: q || undefined,
    page,
  });

  return (
    <>
      <PageHeader title="Hồ sơ thú" description="Tất cả thú do trung tâm tiếp nhận." />
      <Toolbar
        end={
          <div className="pet-search">
            <Search className="pet-search__icon" aria-hidden="true" />
            <label htmlFor="pet-search" className="sr-only">
              Tìm theo tên hoặc mã
            </label>
            <input
              id="pet-search"
              type="search"
              className="pet-search__input"
              placeholder="Tìm theo tên hoặc mã"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
            <label htmlFor="pet-species" className="sr-only">
              Loài
            </label>
            <select
              id="pet-species"
              className="pet-search__select"
              value={species}
              onChange={(e) => setSpecies(e.target.value as Species | "")}
            >
              <option value="">Mọi loài</option>
              <option value="DOG">Chó</option>
              <option value="CAT">Mèo</option>
            </select>
          </div>
        }
      >
        <Segmented label="Lọc theo trạng thái" options={STATUS_OPTIONS} value={status} onChange={setStatus} />
      </Toolbar>
      <ListView
        caption="Danh sách hồ sơ thú"
        data={data}
        loading={loading}
        error={error}
        onRetry={reload}
        onPage={setPage}
        emptyIcon={PawPrint}
        emptyTitle="Không tìm thấy thú phù hợp"
        emptyDescription="Thử bỏ bớt bộ lọc hoặc tìm bằng mã hồ sơ."
        rowKey={(p) => p.id}
        rowHref={(p) => `/staff/pets/${p.id}`}
        columns={[
          { key: "name", header: "Tên", render: (p) => `${petName(p.name)} · ${p.code}` },
          { key: "species", header: "Loài", render: (p) => SPECIES[p.species] },
          { key: "status", header: "Trạng thái", render: (p) => <StatusBadge map={PET_STATUS} value={p.status} /> },
          { key: "area", header: "Khu", render: (p) => (p.area ? AREA[p.area] : "—"), hideOnMobile: true },
          { key: "location", header: "Vị trí", render: (p) => p.currentLocation ?? "—", hideOnMobile: true },
          { key: "created", header: "Tiếp nhận", render: (p) => formatDate(p.createdAt), hideOnMobile: true },
        ]}
      />
    </>
  );
}
