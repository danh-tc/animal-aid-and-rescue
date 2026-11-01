import type { ReactNode } from "react";
import { RequireRole } from "@/components/require-role/RequireRole";

export default function AdopterLayout({ children }: { children: ReactNode }) {
  return (
    <RequireRole roles={["ADOPTER"]}>
      <div className="container page-area">{children}</div>
    </RequireRole>
  );
}
