"use client";

import { Pencil, UserPlus, Users } from "lucide-react";
import { useState } from "react";
import { Badge } from "@/components/badge/Badge";
import { Button } from "@/components/button/Button";
import { Dialog } from "@/components/dialog/Dialog";
import { CheckField, Field } from "@/components/form-field/Field";
import { ListView } from "@/components/list-view/ListView";
import { PageHeader } from "@/components/page-header/PageHeader";
import { PasswordInput } from "@/components/password-input/PasswordInput";
import { Segmented } from "@/components/segmented/Segmented";
import { useToast } from "@/components/toast/Toast";
import { Toolbar } from "@/components/toolbar/Toolbar";
import { useApi, useMutation } from "@/hooks/useApi";
import { usePageParam, useUrlState } from "@/hooks/useUrlState";
import { useAuth } from "@/lib/auth-context";
import { formatDate } from "@/lib/format";
import { ROLE } from "@/lib/labels";
import type { PageResponse, Role, UserAdminView } from "@/types/api";
import "../../staff/detail-page.scss";

type Filter = Role | "ALL";
const EMPTY = { email: "", fullName: "", phone: "", address: "", role: "STAFF" as Role, active: true, password: "" };

export default function UsersPage() {
  const { user: me } = useAuth();
  const [role, setRole] = useUrlState<Filter>("role", "ALL");
  const [page, setPage] = usePageParam();
  const { data, error, loading, reload } = useApi<PageResponse<UserAdminView>>("/api/admin/users", {
    role: role === "ALL" ? undefined : role,
    page,
  });
  const mutate = useMutation();
  const toast = useToast();
  const [editing, setEditing] = useState<UserAdminView | "new" | null>(null);
  const [form, setForm] = useState(EMPTY);

  function openNew() {
    setForm(EMPTY);
    setEditing("new");
  }

  function openEdit(u: UserAdminView) {
    setForm({ email: u.email, fullName: u.fullName, phone: u.phone ?? "", address: u.address ?? "", role: u.role, active: u.active, password: "" });
    setEditing(u);
  }

  async function save() {
    if (form.phone && !/^[0-9+]{9,15}$/.test(form.phone.trim())) throw new Error("Số điện thoại gồm 9 đến 15 chữ số.");
    if (form.password && (form.password.length < 8 || form.password.length > 72)) throw new Error("Mật khẩu dài từ 8 đến 72 ký tự.");
    if (editing === "new") {
      if (!form.password) throw new Error("Nhập mật khẩu ban đầu.");
      await mutate("/api/admin/users", {
        body: { email: form.email.trim(), password: form.password, fullName: form.fullName.trim(), phone: form.phone.trim() || null, role: form.role },
      });
      toast.success("Đã tạo tài khoản.");
    } else if (editing) {
      await mutate(`/api/admin/users/${editing.id}`, {
        method: "PATCH",
        body: {
          fullName: form.fullName.trim() || null,
          phone: form.phone.trim() || null,
          address: form.address.trim() || null,
          role: form.role !== editing.role ? form.role : null,
          active: form.active !== editing.active ? form.active : null,
          password: form.password || null,
        },
      });
      toast.success("Đã cập nhật tài khoản.");
    }
    setEditing(null);
    reload();
  }

  const isAdopter = editing !== "new" && editing?.role === "ADOPTER";
  const isSelf = editing !== "new" && editing?.id === me?.id;

  return (
    <>
      <PageHeader
        title="Tài khoản và phân quyền"
        description="Tạo tài khoản nhân viên, đổi vai trò, khóa tài khoản. Người nhận nuôi tự đăng ký."
        actions={
          <Button icon={<UserPlus aria-hidden="true" />} onClick={openNew}>
            Thêm nhân viên
          </Button>
        }
      />
      <Toolbar>
        <Segmented
          label="Lọc theo vai trò"
          value={role}
          onChange={setRole}
          options={[
            { value: "ALL", label: "Tất cả" },
            { value: "STAFF", label: "Nhân viên" },
            { value: "MANAGER", label: "Quản lý" },
            { value: "ADOPTER", label: "Người nhận nuôi" },
          ]}
        />
      </Toolbar>
      <ListView
        caption="Danh sách tài khoản"
        data={data}
        loading={loading}
        error={error}
        onRetry={reload}
        onPage={setPage}
        emptyIcon={Users}
        emptyTitle="Không có tài khoản"
        rowKey={(u) => u.id}
        columns={[
          { key: "name", header: "Họ tên", render: (u) => u.fullName },
          { key: "email", header: "Email", render: (u) => u.email },
          { key: "role", header: "Vai trò", render: (u) => ROLE[u.role] },
          { key: "active", header: "Trạng thái", render: (u) => (u.active ? <Badge tone="success">Hoạt động</Badge> : <Badge tone="neutral">Đã khóa</Badge>) },
          { key: "created", header: "Tạo ngày", render: (u) => formatDate(u.createdAt), hideOnMobile: true },
          {
            key: "actions",
            header: "Thao tác",
            render: (u) => (
              <Button size="sm" variant="ghost" icon={<Pencil aria-hidden="true" />} onClick={() => openEdit(u)}>
                Sửa
              </Button>
            ),
          },
        ]}
      />

      <Dialog
        open={editing !== null}
        onClose={() => setEditing(null)}
        title={editing === "new" ? "Thêm tài khoản nhân viên" : "Sửa tài khoản"}
        submitLabel={editing === "new" ? "Tạo tài khoản" : "Lưu"}
        onSubmit={save}
        size="lg"
      >
        <div className="detail-page__form-grid">
          <Field label="Email" required={editing === "new"}>
            <input type="email" autoComplete="off" value={form.email} disabled={editing !== "new"} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
          </Field>
          <Field label="Họ tên" required>
            <input maxLength={150} value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
          </Field>
          <Field label="Số điện thoại">
            <input type="tel" inputMode="tel" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </Field>
          <Field label="Vai trò" required hint={isAdopter ? "Không đổi được vai trò của người nhận nuôi." : isSelf ? "Không tự đổi vai trò của mình." : undefined}>
            <select value={form.role} disabled={isAdopter || isSelf} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
              {isAdopter && <option value="ADOPTER">{ROLE.ADOPTER}</option>}
              <option value="STAFF">{ROLE.STAFF}</option>
              <option value="MANAGER">{ROLE.MANAGER}</option>
            </select>
          </Field>
          {editing !== "new" && (
            <Field label="Địa chỉ" className="detail-page__full">
              <input maxLength={255} value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
            </Field>
          )}
          <Field
            label={editing === "new" ? "Mật khẩu ban đầu" : "Đặt lại mật khẩu"}
            required={editing === "new"}
            hint={editing === "new" ? "Từ 8 đến 72 ký tự." : "Để trống nếu không đổi."}
            className="detail-page__full"
          >
            <PasswordInput autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          </Field>
        </div>
        {editing !== "new" && (
          <CheckField
            label="Tài khoản đang hoạt động"
            hint={isSelf ? "Không tự khóa tài khoản của mình." : "Bỏ chọn để khóa, người dùng sẽ không đăng nhập được."}
            checked={form.active}
            disabled={isSelf}
            onChange={(v) => setForm({ ...form, active: v })}
          />
        )}
      </Dialog>
    </>
  );
}
