import type { Metadata, Viewport } from "next";
import { Be_Vietnam_Pro } from "next/font/google";
import type { ReactNode } from "react";
import { Providers } from "@/components/providers/Providers";
import "@/styles/main.scss";

// Be Vietnam Pro: sans-serif hiện đại, thiết kế cho tiếng Việt nên dấu hiển thị chuẩn ở mọi cỡ chữ.
const sans = Be_Vietnam_Pro({
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
    <html lang="vi" className={sans.variable}>
      <body>
        <a href="#main" className="skip-link">
          Chuyển đến nội dung chính
        </a>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
