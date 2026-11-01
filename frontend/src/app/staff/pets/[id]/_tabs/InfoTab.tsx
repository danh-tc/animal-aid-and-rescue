"use client";

import { Pencil, Upload } from "lucide-react";
import { useState } from "react";
import { AttachmentList } from "@/components/attachment-list/AttachmentList";
import { Button } from "@/components/button/Button";
import { DetailList } from "@/components/detail-list/DetailList";
import { Dialog } from "@/components/dialog/Dialog";
import { FilePicker, filesValid } from "@/components/file-picker/FilePicker";
import { Field } from "@/components/form-field/Field";
import { Panel } from "@/components/panel/Panel";
import { useToast } from "@/components/toast/Toast";
import { useMutation } from "@/hooks/useApi";
import { ageLabel, formatDate, formatDateTime, todayIso } from "@/lib/format";
import { AREA, SEX, SPECIES } from "@/lib/labels";
import type { AttachmentView, PetArea, PetDetail, Sex, UpdatePetRequest } from "@/types/api";
import { CostPanel } from "./CostPanel";
import "../../../detail-page.scss";

export function InfoTab({ pet, onChanged }: { pet: PetDetail; onChanged: () => void }) {
  const mutate = useMutation();
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [files, setFiles] = useState<File[]>([]);
  const [form, setForm] = useState({
    name: pet.name ?? "",
    sex: pet.sex,
    birth: pet.estimatedBirthDate ?? "",
    color: pet.color ?? "",
    description: pet.description ?? "",
    area: pet.area ?? ("QUARANTINE" as PetArea),
    location: pet.currentLocation ?? "",
  });

  async function save() {
    const body: UpdatePetRequest = {
      name: form.name.trim() || null,
      sex: form.sex,
      estimatedBirthDate: form.birth || null,
      color: form.color.trim() || null,
      description: form.description.trim() || null,
      area: pet.area && form.area !== pet.area ? form.area : null,
      currentLocation: form.location.trim() || null,
    };
    await mutate(`/api/staff/pets/${pet.id}`, { method: "PATCH", body });
    toast.success("Đã lưu thông tin thú.");
    setEditing(false);
    onChanged();
  }

  async function upload() {
    if (files.length === 0) throw new Error("Chọn ít nhất một ảnh.");
    if (!filesValid(files)) throw new Error("Có tệp lớn hơn 5MB.");
    const data = new FormData();
    files.forEach((f) => data.append("files", f));
    const saved = await mutate<AttachmentView[]>(`/api/staff/pets/${pet.id}/attachments`, { body: data });
    toast.success(`Đã tải lên ${saved.length} ảnh.`);
    setFiles([]);
    setUploading(false);
    onChanged();
  }

  return (
    <div className="detail-page">
      <div className="detail-page__main">
        <Panel
          title="Thông tin cơ bản"
          actions={
            <Button size="sm" variant="ghost" icon={<Pencil aria-hidden="true" />} onClick={() => setEditing(true)}>
              Sửa
            </Button>
          }
        >
          <DetailList
            items={[
              { label: "Loài", value: SPECIES[pet.species] },
              { label: "Giới tính", value: SEX[pet.sex] },
              { label: "Ngày sinh ước tính", value: `${formatDate(pet.estimatedBirthDate)} ${ageLabel(pet.estimatedBirthDate) ? `(${ageLabel(pet.estimatedBirthDate)})` : ""}` },
              { label: "Màu lông", value: pet.color },
              { label: "Khu", value: pet.area ? AREA[pet.area] : "—" },
              { label: "Vị trí hiện tại", value: pet.currentLocation },
              { label: "Mô tả", value: pet.description, wide: true },
              { label: "Tạo hồ sơ", value: formatDateTime(pet.createdAt) },
            ]}
          />
        </Panel>
      </div>
      <aside className="detail-page__side">
        <CostPanel petId={pet.id} />
        <Panel
          title="Ảnh"
          description="Ảnh hiện trên trang công khai khi thú sẵn sàng nhận nuôi."
          actions={
            <Button size="sm" variant="secondary" icon={<Upload aria-hidden="true" />} onClick={() => setUploading(true)}>
              Tải ảnh
            </Button>
          }
        >
          <AttachmentList items={pet.photos} />
        </Panel>
      </aside>

      <Dialog open={editing} onClose={() => setEditing(false)} title="Sửa thông tin thú" submitLabel="Lưu" onSubmit={save} size="lg">
        <div className="detail-page__form-grid">
          <Field label="Tên gọi">
            <input maxLength={100} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </Field>
          <Field label="Giới tính">
            <select value={form.sex} onChange={(e) => setForm({ ...form, sex: e.target.value as Sex })}>
              <option value="UNKNOWN">Chưa rõ</option>
              <option value="MALE">Đực</option>
              <option value="FEMALE">Cái</option>
            </select>
          </Field>
          <Field label="Ngày sinh ước tính">
            <input type="date" max={todayIso()} value={form.birth} onChange={(e) => setForm({ ...form, birth: e.target.value })} />
          </Field>
          <Field label="Màu lông">
            <input maxLength={100} value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} />
          </Field>
          <Field label="Khu" hint="Đổi khu sẽ tự ghi vào nhật ký chăm sóc.">
            <select value={form.area} onChange={(e) => setForm({ ...form, area: e.target.value as PetArea })}>
              <option value="QUARANTINE">Khu cách ly</option>
              <option value="GENERAL">Khu chung</option>
            </select>
          </Field>
          <Field label="Vị trí hiện tại">
            <input maxLength={255} value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} />
          </Field>
          <Field label="Mô tả" className="detail-page__full" hint="Hiện trên trang công khai, nên viết về tính cách của bé.">
            <textarea rows={4} maxLength={2000} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </Field>
        </div>
      </Dialog>

      <Dialog open={uploading} onClose={() => setUploading(false)} title="Tải ảnh thú" submitLabel="Tải lên" onSubmit={upload}>
        <FilePicker label="Ảnh" files={files} onChange={setFiles} />
      </Dialog>
    </div>
  );
}
