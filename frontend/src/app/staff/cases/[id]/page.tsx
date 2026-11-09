"use client";

import { CheckCircle2, UserCheck, XCircle } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { StatusBadge } from "@/components/badge/Badge";
import { Button, LinkButton } from "@/components/button/Button";
import { ChoiceGroup } from "@/components/choice-group/ChoiceGroup";
import { DetailList } from "@/components/detail-list/DetailList";
import { Dialog } from "@/components/dialog/Dialog";
import { FilePicker, filesValid } from "@/components/file-picker/FilePicker";
import { Field } from "@/components/form-field/Field";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Panel } from "@/components/panel/Panel";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { useStaffMembers } from "@/hooks/useStaffMembers";
import { useAuth } from "@/lib/auth-context";
import { formatDateTime, petName, todayIso } from "@/lib/format";
import { CASE_STATUS, PRIORITY, REPORT_STATUS } from "@/lib/labels";
import type { CaseDetail, PageResponse, PetSummary, Priority, Sex, Species, SucceedRequest, SucceedResult } from "@/types/api";
import "../../detail-page.scss";

const EMPTY_PET = { name: "", species: "DOG" as Species, sex: "UNKNOWN" as Sex, birth: "", color: "", description: "" };
const EMPTY_INTAKE = { reason: "", priority: "NEED_CHECK" as Priority, assessment: "" };

export default function CaseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: c, error, loading, reload } = useApi<CaseDetail>(`/api/staff/rescue-cases/${id}`);
  const { members, nameOf } = useStaffMembers();
  const { user } = useAuth();
  const isManager = user?.role === "MANAGER";
  const mutate = useMutation();
  const toast = useToast();
  const [dialog, setDialog] = useState<"assign" | "succeed" | "fail" | null>(null);
  const [assignee, setAssignee] = useState("");
  const [reason, setReason] = useState("");
  const [pet, setPet] = useState(EMPTY_PET);
  const [intake, setIntake] = useState(EMPTY_INTAKE);
  const [result, setResult] = useState<SucceedResult | null>(null);
  const [photos, setPhotos] = useState<File[]>([]);

  const [kind, setKind] = useState<"new" | "lost">("new");
  const [lostPetId, setLostPetId] = useState("");
  const lostPets = useApi<PageResponse<PetSummary>>(dialog === "succeed" ? "/api/staff/pets" : null, {
    status: "LOST",
    size: 100,
  });

  if (loading && !c) return <Skeleton card lines={6} />;
  if (!c) return <Notice tone="error">{error}</Notice>;
  const canClose = isManager || c.assignedStaffId === user?.id;

  async function assign() {
    if (!assignee) throw new Error("Chọn nhân viên phụ trách.");
    await mutate(`/api/staff/rescue-cases/${id}/assign`, { body: { staffId: Number(assignee) } });
    toast.success("Đã phân công ca cứu hộ.");
    setDialog(null);
    reload();
  }

  async function succeed() {
    if (kind === "lost" && !lostPetId) throw new Error("Chọn hồ sơ thú thất lạc đã đối chiếu.");
    const body: SucceedRequest = {
      ...(kind === "lost"
        ? { lostPetId: Number(lostPetId) }
        : {
            pet: {
              name: pet.name.trim() || null,
              species: pet.species,
              sex: pet.sex,
              estimatedBirthDate: pet.birth || null,
              color: pet.color.trim() || null,
              description: pet.description.trim() || null,
            },
          }),
      intake: {
        reason: intake.reason.trim() || null,
        priority: intake.priority,
        initialAssessment: intake.assessment.trim() || null,
      },
    };
    if (!filesValid(photos)) throw new Error("Có ảnh lớn hơn 5MB.");

    const data = new FormData();
    data.append("data", new Blob([JSON.stringify(body)], { type: "application/json" }));
    photos.forEach((p) => data.append("photos", p));
    const res = await mutate<SucceedResult>(`/api/staff/rescue-cases/${id}/succeed`, { body: data });
    setPhotos([]);
    toast.success(
      kind === "lost" ? `Đã ghi nhận tìm lại thú ${res.petCode}, thú vào khu cách ly.` : `Đã tạo hồ sơ thú ${res.petCode}.`,
    );
    setResult(res);
    setDialog(null);
    reload();
  }

  async function fail() {
    await mutate(`/api/staff/rescue-cases/${id}/fail`, { body: { reason: reason.trim() } });
    toast.success("Đã ghi nhận ca cứu hộ thất bại.");
    setDialog(null);
    reload();
  }

  return (
    <>
      <PageHeader
        back={{ href: "/staff/cases", label: "Ca cứu hộ" }}
        title={`Ca cứu hộ #${c.id}`}
        meta={
          <>
            <StatusBadge map={CASE_STATUS} value={c.status} />
            <span>Tạo lúc {formatDateTime(c.createdAt)}</span>
          </>
        }
        actions={
          <>
            {(c.status === "NEW" || c.status === "IN_PROGRESS") && isManager && (
              <Button
                variant={c.status === "IN_PROGRESS" ? "ghost" : undefined}
                icon={<UserCheck aria-hidden="true" />}
                onClick={() => {
                  setAssignee(c.assignedStaffId ? String(c.assignedStaffId) : "");
                  setDialog("assign");
                }}
              >
                {c.status === "IN_PROGRESS" ? "Phân công lại" : "Phân công"}
              </Button>
            )}
            {c.status === "IN_PROGRESS" && canClose && (
              <>
                <Button variant="ghost" icon={<XCircle aria-hidden="true" />} onClick={() => setDialog("fail")}>
                  Thất bại
                </Button>
                <Button icon={<CheckCircle2 aria-hidden="true" />} onClick={() => setDialog("succeed")}>
                  Cứu thành công
                </Button>
              </>
            )}
          </>
        }
      />

      <div className="detail-page">
        <div className="detail-page__main">
          {c.status === "NEW" && !isManager && (
            <Notice tone="info" title="Chờ Quản lý phân công">
              Chỉ Quản lý được phân công người đi cứu. Sau khi phân công, nhân viên phụ trách cập nhật kết quả ca.
            </Notice>
          )}
          {c.status === "IN_PROGRESS" && !canClose && (
            <Notice tone="info" title="Ca đã được phân công cho nhân viên khác">
              Bạn có thể xem thông tin ca nhưng chỉ nhân viên phụ trách hoặc Quản lý được ghi kết quả thành công hay thất bại.
            </Notice>
          )}
          {result?.urgentVetRequestSuggested && (
            <Notice
              tone="warning"
              title="Thú ở mức Khẩn cấp"
              action={<LinkButton href={`/staff/pets/${result.petId}?tab=vet`} size="sm">Tạo yêu cầu thú y</LinkButton>}
            >
              Nên tạo yêu cầu thú y ngay cho {result.petCode}.
            </Notice>
          )}
          <Panel title="Thông tin ca">
            <DetailList
              items={[
                { label: "Vị trí", value: c.locationText, wide: true },
                { label: "Phụ trách", value: nameOf(c.assignedStaffId) },
                { label: "Người tạo", value: nameOf(c.createdBy) },
                { label: "Đóng lúc", value: formatDateTime(c.closedAt) },
                ...(c.failureReason ? [{ label: "Lý do thất bại", value: c.failureReason, wide: true }] : []),
              ]}
            />
          </Panel>
        </div>
        <aside className="detail-page__side">
          {c.petId && (
            <Panel title="Hồ sơ thú">
              <LinkButton href={`/staff/pets/${c.petId}`} variant="secondary" block>
                Mở hồ sơ {c.petCode}
              </LinkButton>
            </Panel>
          )}
          <Panel title="Báo cáo nguồn">
            <ul className="detail-page__links">
              {c.reports.map((r) => (
                <li key={r.id}>
                  <Link href={`/staff/reports/${r.id}`} className="detail-page__link">
                    <span>{r.code}</span>
                    <StatusBadge map={REPORT_STATUS} value={r.status} />
                    <span className="detail-page__link-meta">{r.reporterName} · {r.reporterPhone}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </Panel>
        </aside>
      </div>

      <Dialog
        open={dialog === "assign"}
        onClose={() => setDialog(null)}
        title={c.status === "IN_PROGRESS" ? "Phân công lại ca cứu hộ" : "Phân công ca cứu hộ"}
        submitLabel={c.status === "IN_PROGRESS" ? "Lưu phân công" : "Phân công"}
        onSubmit={assign}
      >
        <Field label="Nhân viên phụ trách" required>
          <select value={assignee} onChange={(e) => setAssignee(e.target.value)} required>
            <option value="">Chọn nhân viên</option>
            {members.map((m) => (
              <option key={m.id} value={m.id}>
                {m.fullName}
              </option>
            ))}
          </select>
        </Field>
      </Dialog>

      <Dialog
        open={dialog === "fail"}
        onClose={() => setDialog(null)}
        title="Ghi nhận ca cứu hộ thất bại"
        submitLabel="Xác nhận"
        submitVariant="danger"
        onSubmit={fail}
      >
        <Field label="Lý do" required hint="Ví dụ: không tìm thấy con vật tại địa điểm.">
          <textarea rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} required />
        </Field>
      </Dialog>

      <Dialog
        open={dialog === "succeed"}
        onClose={() => setDialog(null)}
        title="Cứu thành công"
        description="Hệ thống tạo lượt tiếp nhận và đưa thú vào khu cách ly."
        submitLabel={kind === "lost" ? "Ghi nhận tìm lại" : "Tạo hồ sơ"}
        onSubmit={succeed}
        size="lg"
      >
        <ChoiceGroup
          legend="Thú được cứu"
          required
          columns={2}
          value={kind}
          onChange={setKind}
          choices={[
            { value: "new", label: "Thú mới", hint: "Tạo hồ sơ thú mới" },
            { value: "lost", label: "Thú thất lạc của trung tâm", hint: "Đã đối chiếu ảnh, đặc điểm và xác nhận đúng con" },
          ]}
        />
        {kind === "lost" && (
          <Field label="Hồ sơ thú thất lạc" required hint="Hệ thống tạo lượt tìm lại trên hồ sơ cũ, không tạo hồ sơ mới.">
            <select value={lostPetId} onChange={(e) => setLostPetId(e.target.value)} required>
              <option value="">Chọn thú</option>
              {lostPets.data?.items.map((p) => (
                <option key={p.id} value={p.id}>
                  {petName(p.name, p.code)} · {p.code}
                </option>
              ))}
            </select>
          </Field>
        )}
        {kind === "new" && <h3 className="detail-page__form-section">Thông tin thú</h3>}
        {kind === "new" && (
          <div className="detail-page__form-grid">
            <Field label="Loài" required>
              <select value={pet.species} onChange={(e) => setPet({ ...pet, species: e.target.value as Species })}>
                <option value="DOG">Chó</option>
                <option value="CAT">Mèo</option>
              </select>
            </Field>
            <Field label="Giới tính">
              <select value={pet.sex} onChange={(e) => setPet({ ...pet, sex: e.target.value as Sex })}>
                <option value="UNKNOWN">Chưa rõ</option>
                <option value="MALE">Đực</option>
                <option value="FEMALE">Cái</option>
              </select>
            </Field>
            <Field label="Tên gọi" hint="Để trống nếu chưa đặt tên.">
              <input maxLength={100} value={pet.name} onChange={(e) => setPet({ ...pet, name: e.target.value })} />
            </Field>
            <Field label="Ngày sinh ước tính">
              <input type="date" max={todayIso()} value={pet.birth} onChange={(e) => setPet({ ...pet, birth: e.target.value })} />
            </Field>
            <Field label="Màu lông" className="detail-page__full">
              <input maxLength={100} value={pet.color} onChange={(e) => setPet({ ...pet, color: e.target.value })} />
            </Field>
            <Field label="Mô tả" className="detail-page__full">
              <textarea rows={2} maxLength={2000} value={pet.description} onChange={(e) => setPet({ ...pet, description: e.target.value })} />
            </Field>
          </div>
        )}
        <FilePicker label="Ảnh thú" files={photos} onChange={setPhotos} hint="Không bắt buộc. JPG, PNG hoặc WEBP, tối đa 5 ảnh, mỗi ảnh không quá 5MB." />
        <h3 className="detail-page__form-section">Tiếp nhận</h3>
        <div className="detail-page__form-grid">
          <Field label="Mức ưu tiên" required>
            <select value={intake.priority} onChange={(e) => setIntake({ ...intake, priority: e.target.value as Priority })}>
              {(Object.keys(PRIORITY) as Priority[]).map((p) => (
                <option key={p} value={p}>
                  {PRIORITY[p].label}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Lý do tiếp nhận">
            <input maxLength={1000} value={intake.reason} onChange={(e) => setIntake({ ...intake, reason: e.target.value })} />
          </Field>
          <Field label="Đánh giá ban đầu" className="detail-page__full">
            <textarea rows={2} maxLength={2000} value={intake.assessment} onChange={(e) => setIntake({ ...intake, assessment: e.target.value })} />
          </Field>
        </div>
      </Dialog>
    </>
  );
}
