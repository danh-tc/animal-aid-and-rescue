import type { Metadata, Viewport } from "next";
import { Nunito, Nunito_Sans } from "next/font/google";
import type { ReactNode } from "react";
import { Providers } from "@/components/providers/Providers";
import "@/styles/main.scss";

// Font theo gợi ý ui-ux-pro-max cho nhóm thú cưng: bo tròn, thân thiện, có bộ chữ tiếng Việt.
const heading = Nunito({
  subsets: ["latin", "vietnamese"],
  weight: ["600", "700", "800"],
  variable: "--font-heading",
  display: "swap",
});
const body = Nunito_Sans({
  subsets: ["latin", "vietnamese"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-body",
  display: "swap",
});

export const metadata: Metadata = {
  title: { default: "Animal Aid and Rescue", template: "%s · Animal Aid and Rescue" },
  description: "Cứu hộ, chăm sóc và tìm mái nhà mới cho chó mèo bị bỏ rơi",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f5f9f8" },
    { media: "(prefers-color-scheme: dark)", color: "#0b1614" },
  ],
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="vi" className={`${heading.variable} ${body.variable}`}>
      <body>
        <a href="#main" className="skip-link">
          Bỏ qua tới nội dung chính
        </a>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
