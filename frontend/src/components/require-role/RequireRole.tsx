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
