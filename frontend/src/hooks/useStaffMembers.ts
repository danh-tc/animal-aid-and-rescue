"use client";

import { useMemo } from "react";
import { useApi } from "@/hooks/useApi";
import type { StaffMember } from "@/types/api";

/** Danh sách nhân viên đang hoạt động, dùng cho ô phân công và hiển thị tên theo id. */
export function useStaffMembers() {
  const { data } = useApi<StaffMember[]>("/api/staff/staff-members");
  const names = useMemo(() => new Map((data ?? []).map((s) => [s.id, s.fullName])), [data]);
  return { members: data ?? [], nameOf: (id: number | null | undefined) => (id ? (names.get(id) ?? `#${id}`) : "—") };
}
