import type { Metadata } from "next";
import type { ReactNode } from "react";
import { SiteHeader } from "@/components/site-header/SiteHeader";
import "@/components/button/button.scss";
import "@/styles/main.scss";

export const metadata: Metadata = {
  title: "Animal Aid and Rescue",
  description: "Cứu hộ và nhận nuôi chó mèo",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="vi">
      <body>
        <SiteHeader />
        <main>{children}</main>
      </body>
    </html>
  );
}
