import { type ReactNode, Suspense } from "react";
import { AppShell } from "@/components/app-shell/AppShell";
import { RequireRole } from "@/components/require-role/RequireRole";
import { Skeleton } from "@/components/skeleton/Skeleton";

export default function StaffLayout({ children }: { children: ReactNode }) {
  return (
    <RequireRole roles={["STAFF", "MANAGER"]}>
      <AppShell>
        <Suspense fallback={<Skeleton lines={5} />}>{children}</Suspense>
      </AppShell>
    </RequireRole>
  );
}
