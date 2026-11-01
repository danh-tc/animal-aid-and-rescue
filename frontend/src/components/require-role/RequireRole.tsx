"use client";

import { useRouter } from "next/navigation";
import { type ReactNode, useEffect } from "react";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { homeFor, useAuth } from "@/lib/auth-context";
import type { Role } from "@/types/api";

interface RequireRoleProps {
  roles: Role[];
  children: ReactNode;
}

/**
 * Chặn trang theo vai trò ở phía giao diện. Backend vẫn kiểm tra quyền qua JWT,
 * lớp này chỉ để người dùng không thấy trang không dành cho mình.
 */
export function RequireRole({ roles, children }: RequireRoleProps) {
  const { ready, user } = useAuth();
  const router = useRouter();
  const allowed = !!user && roles.includes(user.role);

  useEffect(() => {
    if (!ready || allowed) return;
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent(window.location.pathname + window.location.search)}`);
    } else {
      router.replace(homeFor(user.role));
    }
  }, [ready, allowed, user, router]);

  if (!ready || !allowed) {
    return (
      <div className="container" aria-busy="true">
        <Skeleton lines={4} />
      </div>
    );
  }
  return <>{children}</>;
}
