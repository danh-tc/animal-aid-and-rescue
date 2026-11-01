import { type ReactNode, Suspense } from "react";
import { AppShell } from "@/components/app-shell/AppShell";
import { RequireRole } from "@/components/require-role/RequireRole";

export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <RequireRole roles={["MANAGER"]}>
      <AppShell>
        <Suspense>{children}</Suspense>
      </AppShell>
    </RequireRole>
  );
}
