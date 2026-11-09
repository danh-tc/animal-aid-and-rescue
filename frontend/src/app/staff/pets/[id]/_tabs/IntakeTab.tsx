import Link from "next/link";
import { StatusBadge } from "@/components/badge/Badge";
import { Panel } from "@/components/panel/Panel";
import { Timeline } from "@/components/timeline/Timeline";
import { formatDateTime } from "@/lib/format";
import { INTAKE_SOURCE, PRIORITY } from "@/lib/labels";
import type { PetDetail } from "@/types/api";

export function IntakeTab({ pet }: { pet: PetDetail }) {
  return (
    <Panel title="Các lượt tiếp nhận" description="Mỗi lần thú vào trung tâm, kể cả khi được trả lại.">
      <Timeline
        entries={pet.intakes.map((i) => ({
          key: i.id,
          title: (
            <>
              {INTAKE_SOURCE[i.source]} <StatusBadge map={PRIORITY} value={i.priority} />
            </>
          ),
          time: formatDateTime(i.intakeAt),
          body: (
            <>
              {i.reason && <div>Lý do: {i.reason}</div>}
              {i.initialAssessment && <div>Đánh giá ban đầu: {i.initialAssessment}</div>}
              {i.rescueCaseId && <Link href={`/staff/cases/${i.rescueCaseId}`}>Ca cứu hộ #{i.rescueCaseId}</Link>}
            </>
          ),
        }))}
      />
    </Panel>
  );
}
