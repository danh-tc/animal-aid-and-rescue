import type { ReactNode } from "react";

/** Đăng nhập, đăng ký dùng bố cục toàn màn hình riêng (AuthShell), không có header và footer công khai. */
export default function AuthLayout({ children }: { children: ReactNode }) {
  return <main id="main" tabIndex={-1}>{children}</main>;
}
