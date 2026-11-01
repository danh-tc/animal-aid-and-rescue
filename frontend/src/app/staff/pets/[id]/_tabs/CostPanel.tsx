import { CostSummary } from "@/components/cost-summary/CostSummary";
import { Panel } from "@/components/panel/Panel";

export function CostPanel({ petId }: { petId: number }) {
  return (
    <Panel title="Chi phí và ủng hộ" description="Cộng dồn qua mọi lần điều trị và bàn giao.">
      <CostSummary petId={petId} />
    </Panel>
  );
}
