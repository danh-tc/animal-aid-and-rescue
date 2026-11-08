"use client";

import { Skeleton } from "@/components/skeleton/Skeleton";
import { useApi } from "@/hooks/useApi";
import { formatMoney } from "@/lib/format";
import type { PetCosts } from "@/types/api";
import "./cost-summary.scss";

export function CostSummary({ petId }: { petId: number }) {
  const { data, error } = useApi<PetCosts>(`/api/staff/pets/${petId}/costs`);
  if (error) return <p className="cost-summary__error">{error}</p>;
  if (!data) return <Skeleton lines={2} />;
  return (
    <dl className="cost-summary">
      <div className="cost-summary__item">
        <dt>Tổng chi phí điều trị</dt>
        <dd>{formatMoney(data.totalTreatmentCost)}</dd>
      </div>
      <div className="cost-summary__item">
        <dt>Đã được ủng hộ</dt>
        <dd>{formatMoney(data.totalDonated)}</dd>
      </div>
      <div className="cost-summary__item cost-summary__item--strong">
        <dt>Còn chưa bù đắp</dt>
        <dd>{formatMoney(data.outstanding)}</dd>
      </div>
    </dl>
  );
}
