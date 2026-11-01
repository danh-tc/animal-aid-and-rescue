"use client";

import { ArrowRight } from "lucide-react";
import { StatusBadge } from "@/components/badge/Badge";
import { Notice } from "@/components/notice/Notice";
import { Panel } from "@/components/panel/Panel";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { Timeline } from "@/components/timeline/Timeline";
import { useApi } from "@/hooks/useApi";
import { useStaffMembers } from "@/hooks/useStaffMembers";
import { formatDateTime } from "@/lib/format";
import { PET_EVENT, PET_STATUS } from "@/lib/labels";
import type { HistoryView, PetStatus } from "@/types/api";

export function HistoryTab({ petId }: { petId: number }) {
  const { data, error, loading } = useApi<HistoryView[]>(`/api/staff/pets/${petId}/history`);
  const { nameOf } = useStaffMembers();

  return (
    <Panel title="Lịch sử trạng thái" description="Ghi lại mọi lần đổi trạng thái, ai đổi và khi nào.">
      {error && <Notice tone="error">{error}</Notice>}
      {loading && !data && <Skeleton lines={4} />}
      {data && (
        <Timeline
          entries={[...data].reverse().map((h, i) => ({
            key: `${h.changedAt}-${i}`,
            title: (
              <>
                {PET_EVENT[h.event] ?? h.event}
                {h.fromStatus && <StatusBadge map={PET_STATUS} value={h.fromStatus as PetStatus} />}
                {h.fromStatus && <ArrowRight aria-label="sang" width={14} height={14} />}
                <StatusBadge map={PET_STATUS} value={h.toStatus as PetStatus} />
              </>
            ),
            time: formatDateTime(h.changedAt),
            body: [h.note, h.changedBy ? `Thực hiện: ${nameOf(h.changedBy)}` : null].filter(Boolean).join("\n"),
          }))}
        />
      )}
    </Panel>
  );
}
