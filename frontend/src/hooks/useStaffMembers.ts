"use client";

import { useMemo } from "react";
import { useApi } from "@/hooks/useApi";
import type { StaffMember } from "@/types/api";

export function useStaffMembers() {
  const { data } = useApi<StaffMember[]>("/api/staff/staff-members");
  const names = useMemo(() => new Map((data ?? []).map((s) => [s.id, s.fullName])), [data]);
  return { members: data ?? [], nameOf: (id: number | null | undefined) => (id ? (names.get(id) ?? `#${id}`) : "—") };
}
