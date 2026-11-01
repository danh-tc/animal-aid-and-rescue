"use client";

import { Hospital, Pencil, Plus } from "lucide-react";
import { useState } from "react";
import { Badge } from "@/components/badge/Badge";
import { Button } from "@/components/button/Button";
import { Dialog } from "@/components/dialog/Dialog";
import { CheckField, Field } from "@/components/form-field/Field";
import { ListView } from "@/components/list-view/ListView";
import { PageHeader } from "@/components/page-header/PageHeader";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import type { ClinicView } from "@/types/api";

const EMPTY = { name: "", address: "", phone: "", active: true };

/** M03: Danh mục cơ sở thú y */
export default function ClinicsPage() {
  const { data, error, loading, reload } = useApi<ClinicView[]>("/api/admin/vet-clinics");
  const mutate = useMutation();
  const toast = useToast();
  const [editing, setEditing] = useState<ClinicView | "new" | null>(null);
  const [form, setForm] = useState(EMPTY);

  function open(c: ClinicView | "new") {
    setForm(c === "new" ? EMPTY : { name: c.name, address: c.address ?? "", phone: c.phone ?? "", active: c.active });
    setEditing(c);
  }

  async function save() {
    const body = { name: form.name.trim(), address: form.address.trim() || null, phone: form.phone.trim() || null, active: form.active };
    if (editing === "new") await mutate("/api/admin/vet-clinics", { body });
    else if (editing) await mutate(`/api/admin/vet-clinics/${editing.id}`, { method: "PATCH", body });
    toast.success(editing === "new" ? "Đã thêm cơ sở thú y." : "Đã cập nhật cơ sở thú y.");
    setEditing(null);
    reload();
  }

  return (
    <>
      <PageHeader
        title="Cơ sở thú y"
        description="Nơi trung tâm gửi thú đi khám. Cơ sở ngừng hợp tác thì tắt hoạt động, không xóa để giữ lịch sử."
        actions={
          <Button icon={<Plus aria-hidden="true" />} onClick={() => open("new")}>
            Thêm cơ sở
          </Button>
        }
      />
      <ListView
        caption="Danh sách cơ sở thú y"
        data={data}
        loading={loading}
        error={error}
        onRetry={reload}
        emptyIcon={Hospital}
        emptyTitle="Chưa có cơ sở thú y"
        rowKey={(c) => c.id}
        columns={[
          { key: "name", header: "Tên", render: (c) => c.name },
          { key: "address", header: "Địa chỉ", render: (c) => c.address ?? "—" },
          { key: "phone", header: "Điện thoại", render: (c) => c.phone ?? "—" },
          { key: "active", header: "Trạng thái", render: (c) => (c.active ? <Badge tone="success">Đang hợp tác</Badge> : <Badge tone="neutral">Ngừng</Badge>) },
          {
            key: "actions",
            header: "Thao tác",
            render: (c) => (
              <Button size="sm" variant="ghost" icon={<Pencil aria-hidden="true" />} onClick={() => open(c)}>
                Sửa
              </Button>
            ),
          },
        ]}
      />
      <Dialog
        open={editing !== null}
        onClose={() => setEditing(null)}
        title={editing === "new" ? "Thêm cơ sở thú y" : "Sửa cơ sở thú y"}
        submitLabel="Lưu"
        onSubmit={save}
      >
        <Field label="Tên cơ sở" required>
          <input maxLength={150} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        </Field>
        <Field label="Địa chỉ">
          <input maxLength={255} value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
        </Field>
        <Field label="Điện thoại">
          <input type="tel" inputMode="tel" maxLength={20} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        </Field>
        <CheckField label="Đang hợp tác" hint="Chỉ cơ sở đang hợp tác mới chọn được khi tạo yêu cầu thú y." checked={form.active} onChange={(v) => setForm({ ...form, active: v })} />
      </Dialog>
    </>
  );
}
