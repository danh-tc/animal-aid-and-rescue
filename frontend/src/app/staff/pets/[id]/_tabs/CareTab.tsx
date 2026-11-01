"use client";

import { type FormEvent, useState } from "react";
import { Button } from "@/components/button/Button";
import { Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { Pagination } from "@/components/pagination/Pagination";
import { Panel } from "@/components/panel/Panel";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { Timeline } from "@/components/timeline/Timeline";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { useStaffMembers } from "@/hooks/useStaffMembers";
import { errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { CARE_ACTIVITY } from "@/lib/labels";
import type { CareActivity, CareLogView, PageResponse, PetDetail } from "@/types/api";
import "../../../detail-page.scss";

// AREA_CHANGE do hệ thống tự ghi khi đổi khu ở tab Thông tin
const ACTIVITIES: CareActivity[] = ["FEEDING", "CLEANING", "WEIGHT", "MEDICATION", "HEALTH_NOTE"];

export function CareTab({ pet }: { pet: PetDetail; onChanged: () => void }) {
  const [page, setPage] = useState(0);
  const { data, error, loading, reload } = useApi<PageResponse<CareLogView>>(`/api/staff/pets/${pet.id}/care-logs`, {
    page,
  });
  const { nameOf } = useStaffMembers();
  const mutate = useMutation();
  const toast = useToast();
  const [activity, setActivity] = useState<CareActivity>("FEEDING");
  const [weight, setWeight] = useState("");
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (activity === "WEIGHT" && !(Number(weight) > 0)) {
      setFormError("Nhập cân nặng lớn hơn 0.");
      return;
    }
    setSaving(true);
    setFormError(null);
    try {
      await mutate(`/api/staff/pets/${pet.id}/care-logs`, {
        body: {
          activityType: activity,
          weightKg: activity === "WEIGHT" ? Number(weight) : null,
          note: note.trim() || null,
        },
      });
      toast.success("Đã ghi nhật ký.");
      setNote("");
      setWeight("");
      setPage(0);
      reload();
    } catch (e) {
      setFormError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="detail-page">
      <div className="detail-page__main">
        <Panel title="Nhật ký chăm sóc">
          {error && <Notice tone="error">{error}</Notice>}
          {loading && !data && <Skeleton lines={4} />}
          {data && data.items.length === 0 && <p>Chưa có nhật ký.</p>}
          {data && (
            <Timeline
              entries={data.items.map((l) => ({
                key: l.id,
                title: (
                  <>
                    {CARE_ACTIVITY[l.activityType]}
                    {l.weightKg !== null && ` · ${l.weightKg} kg`}
                  </>
                ),
                time: formatDateTime(l.loggedAt),
                body: [l.note, `Ghi bởi ${nameOf(l.staffId)}`].filter(Boolean).join("\n"),
              }))}
            />
          )}
        </Panel>
        {data && (
          <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />
        )}
      </div>
      <aside className="detail-page__side">
        <Panel title="Ghi nhật ký mới">
          <form className="stack" onSubmit={submit}>
            {formError && <Notice tone="error">{formError}</Notice>}
            <Field label="Hoạt động" required>
              <select value={activity} onChange={(e) => setActivity(e.target.value as CareActivity)}>
                {ACTIVITIES.map((a) => (
                  <option key={a} value={a}>
                    {CARE_ACTIVITY[a]}
                  </option>
                ))}
              </select>
            </Field>
            {activity === "WEIGHT" && (
              <Field label="Cân nặng (kg)" required>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  max="999.99"
                  inputMode="decimal"
                  value={weight}
                  onChange={(e) => setWeight(e.target.value)}
                />
              </Field>
            )}
            <Field label="Ghi chú">
              <textarea rows={3} maxLength={2000} value={note} onChange={(e) => setNote(e.target.value)} />
            </Field>
            <Button type="submit" loading={saving} block>
              Lưu nhật ký
            </Button>
          </form>
        </Panel>
      </aside>
    </div>
  );
}
