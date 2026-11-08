"use client";

import { Ban, Stethoscope } from "lucide-react";
import { useParams } from "next/navigation";
import { useState } from "react";
import { Badge, StatusBadge } from "@/components/badge/Badge";
import { Button } from "@/components/button/Button";
import { Dialog } from "@/components/dialog/Dialog";
import { Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { TabPanel, Tabs } from "@/components/tabs/Tabs";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { useUrlState } from "@/hooks/useUrlState";
import { petName } from "@/lib/format";
import { AREA, PET_STATUS, SPECIES } from "@/lib/labels";
import type { EndEventView, IneligibleResult, PetDetail } from "@/types/api";
import { CareTab } from "./_tabs/CareTab";
import { ChecklistTab } from "./_tabs/ChecklistTab";
import { HandoverTab } from "./_tabs/HandoverTab";
import { HistoryTab } from "./_tabs/HistoryTab";
import { InfoTab } from "./_tabs/InfoTab";
import { IntakeTab } from "./_tabs/IntakeTab";
import { EndEventNotice, LifecycleActions } from "./_tabs/LifecyclePanel";
import { VetTab } from "./_tabs/VetTab";
import "./pet-record.scss";

type Tab = "info" | "intakes" | "care" | "vet" | "checklist" | "history" | "handovers";
const AT_CENTER = ["IN_CARE", "IN_TREATMENT", "AVAILABLE", "PENDING_HANDOVER"];

export default function PetRecordPage() {
  const { id } = useParams<{ id: string }>();
  const [tab, setTab] = useUrlState<Tab>("tab", "info");
  const { data: pet, error, loading, reload: reloadPet } = useApi<PetDetail>(`/api/staff/pets/${id}`);
  const endEvents = useApi<EndEventView[]>(`/api/staff/pets/${id}/end-events`);
  const reload = () => {
    reloadPet();
    endEvents.reload();
  };
  const mutate = useMutation();
  const toast = useToast();
  const [ineligibleOpen, setIneligibleOpen] = useState(false);
  const [reason, setReason] = useState("");

  if (loading && !pet) return <Skeleton card lines={8} />;
  if (!pet) return <Notice tone="error">{error}</Notice>;

  const missing = pet.checklist.items.filter((i) => !i.passed).length;
  const openVet = pet.vetRequests.filter((v) => v.status === "PENDING" || v.status === "IN_TREATMENT").length;
  const canMarkIneligible = pet.status === "AVAILABLE" || pet.status === "PENDING_HANDOVER";

  async function markIneligible() {
    const res = await mutate<IneligibleResult>(`/api/staff/pets/${id}/ineligible`, { body: { reason: reason.trim() } });
    toast.success(
      res.cancelledApplications > 0
        ? `Đã chuyển về Đang chăm sóc và hủy ${res.cancelledApplications} đơn liên quan.`
        : "Đã chuyển về Đang chăm sóc.",
    );
    setIneligibleOpen(false);
    setReason("");
    reload();
  }

  return (
    <>
      <PageHeader
        back={{ href: "/staff/pets", label: "Hồ sơ thú" }}
        title={petName(pet.name, pet.code)}
        meta={
          <>
            <span>{pet.code}</span>
            <span>{SPECIES[pet.species]}</span>
            <StatusBadge map={PET_STATUS} value={pet.status} />
            {pet.area && <Badge tone={pet.area === "QUARANTINE" ? "warning" : "neutral"}>{AREA[pet.area]}</Badge>}
          </>
        }
        actions={
          <>
            <LifecycleActions pet={pet} onChanged={reload} />
            {canMarkIneligible && (
              <Button variant="ghost" icon={<Ban aria-hidden="true" />} onClick={() => setIneligibleOpen(true)}>
                Không đủ điều kiện
              </Button>
            )}
            {AT_CENTER.includes(pet.status) && (
              <Button variant="secondary" icon={<Stethoscope aria-hidden="true" />} onClick={() => setTab("vet")}>
                Yêu cầu thú y
              </Button>
            )}
          </>
        }
      />

      <div className="pet-record__alert">
        <EndEventNotice pet={pet} events={endEvents.data} />
      </div>

      <Tabs
        label="Các phần của hồ sơ"
        value={tab}
        onChange={(t) => setTab(t as Tab)}
        items={[
          { id: "info", label: "Thông tin" },
          { id: "intakes", label: "Tiếp nhận", count: pet.intakes.length },
          { id: "care", label: "Nhật ký" },
          { id: "vet", label: "Thú y", count: openVet || undefined },
          { id: "checklist", label: "Checklist", count: missing || undefined },
          { id: "history", label: "Lịch sử" },
          { id: "handovers", label: "Bàn giao" },
        ]}
      />

      <TabPanel id={tab}>
        {tab === "info" && <InfoTab pet={pet} onChanged={reload} />}
        {tab === "intakes" && <IntakeTab pet={pet} />}
        {tab === "care" && <CareTab pet={pet} onChanged={reload} />}
        {tab === "vet" && <VetTab pet={pet} onChanged={reload} />}
        {tab === "checklist" && <ChecklistTab pet={pet} onChanged={reload} />}
        {tab === "history" && <HistoryTab petId={pet.id} />}
        {tab === "handovers" && <HandoverTab pet={pet} onChanged={reload} />}
      </TabPanel>

      <Dialog
        open={ineligibleOpen}
        onClose={() => setIneligibleOpen(false)}
        title="Đánh dấu không đủ điều kiện?"
        description="Thú về trạng thái Đang chăm sóc. Các đơn đang chờ hoặc đã duyệt sẽ bị hủy."
        submitLabel="Xác nhận"
        submitVariant="danger"
        onSubmit={markIneligible}
      >
        <Field label="Lý do" required>
          <textarea rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} required />
        </Field>
      </Dialog>
    </>
  );
}
