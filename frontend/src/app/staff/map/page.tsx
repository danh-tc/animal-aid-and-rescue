"use client";

import { MapPinOff, RotateCw } from "lucide-react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { Button } from "@/components/button/Button";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { type MarkerKind, markerKind, pointKey } from "@/components/rescue-map/markerKind";
import { Segmented } from "@/components/segmented/Segmented";
import { Toolbar } from "@/components/toolbar/Toolbar";
import { useApi } from "@/hooks/useApi";
import { useUrlState } from "@/hooks/useUrlState";
import { formatRelative } from "@/lib/format";
import type { MapPoint, RescueMapData } from "@/types/api";
import "@/components/rescue-map/rescue-map.scss";

// Leaflet touches window on import.
const RescueMap = dynamic(() => import("@/components/rescue-map/RescueMap"), {
  ssr: false,
  loading: () => <div className="rescue-map rescue-map--loading" aria-hidden="true" />,
});

type Filter = MarkerKind | "ALL";

const LEGEND: { kind: MarkerKind; label: string }[] = [
  { kind: "report", label: "Báo cáo mới" },
  { kind: "case-new", label: "Ca chưa phân công" },
  { kind: "case-active", label: "Ca đang xử lý" },
];

export default function RescueMapPage() {
  const router = useRouter();
  const [filter, setFilter] = useUrlState<Filter>("show", "ALL");
  const [selected, setSelected] = useState<string | null>(null);
  const { data, error, loading, reload } = useApi<RescueMapData>("/api/staff/rescue-map");

  const counts = useMemo(() => {
    const c: Record<MarkerKind, number> = { report: 0, "case-new": 0, "case-active": 0 };
    for (const p of data?.points ?? []) c[markerKind(p)]++;
    return c;
  }, [data]);

  const points = useMemo(
    () => (data?.points ?? []).filter((p) => filter === "ALL" || markerKind(p) === filter),
    [data, filter],
  );

  function open(p: MapPoint) {
    router.push(p.kind === "REPORT" ? `/staff/reports/${p.id}` : `/staff/cases/${p.id}`);
  }

  return (
    <>
      <PageHeader
        title="Bản đồ cứu hộ"
        description="Báo cáo mới và ca đang mở theo vị trí. Các điểm gần nhau có thể là cùng một con vật, nên kiểm tra trước khi tạo ca mới."
        actions={
          <Button variant="ghost" onClick={reload} loading={loading} icon={<RotateCw aria-hidden="true" />}>
            Tải lại
          </Button>
        }
      />
      <Toolbar>
        <Segmented
          label="Lọc điểm trên bản đồ"
          value={filter}
          onChange={setFilter}
          options={[
            { value: "ALL", label: `Tất cả (${data?.points.length ?? 0})` },
            ...LEGEND.map((l) => ({ value: l.kind, label: `${l.label} (${counts[l.kind]})` })),
          ]}
        />
      </Toolbar>

      {error && (
        <Notice tone="error" action={<Button variant="ghost" onClick={reload}>Thử lại</Button>}>
          {error}
        </Notice>
      )}

      <div className="rescue-map-layout">
        <div className="rescue-map-layout__map">
          {data ? (
            <RescueMap points={points} selected={selected} onSelect={setSelected} onOpen={open} />
          ) : (
            <div className="rescue-map rescue-map--loading" aria-hidden="true" />
          )}
          <ul className="rescue-map-legend" aria-label="Chú thích">
            {LEGEND.map((l) => (
              <li key={l.kind}>
                <span className={`rescue-map-legend__dot rescue-map-legend__dot--${l.kind}`} aria-hidden="true" />
                {l.label}
              </li>
            ))}
          </ul>
        </div>

        <aside className="rescue-map-list" aria-label="Danh sách điểm trên bản đồ">
          <p className="rescue-map-list__title">
            {points.length} điểm{filter !== "ALL" && " theo bộ lọc"}
          </p>
          {data && points.length === 0 && (
            <div className="rescue-map-list__empty">
              <MapPinOff aria-hidden="true" />
              Không có điểm nào cần xử lý.
            </div>
          )}
          <ul>
            {points.map((p) => {
              const key = pointKey(p);
              const kind = markerKind(p);
              return (
                <li key={key}>
                  <button
                    type="button"
                    className={`rescue-map-list__item${key === selected ? " rescue-map-list__item--active" : ""}`}
                    aria-pressed={key === selected}
                    onClick={() => setSelected(key)}
                  >
                    <span className={`rescue-map-legend__dot rescue-map-legend__dot--${kind}`} aria-hidden="true" />
                    <span className="rescue-map-list__text">
                      <span className="rescue-map-list__label">{p.label}</span>
                      <span className="rescue-map-list__location">{p.locationText}</span>
                    </span>
                    <span className="rescue-map-list__time">{formatRelative(p.createdAt)}</span>
                  </button>
                </li>
              );
            })}
          </ul>
          {data && data.withoutCoordinates > 0 && (
            <p className="rescue-map-list__missing">
              {data.withoutCoordinates} mục chưa có tọa độ nên không hiện trên bản đồ.{" "}
              <Link href="/staff/reports">Xem danh sách báo cáo</Link>
            </p>
          )}
        </aside>
      </div>
    </>
  );
}
