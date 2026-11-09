import { PawPrint } from "lucide-react";
import Link from "next/link";
import "./site-footer.scss";

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="container site-footer__inner">
        <div className="site-footer__brand">
          <PawPrint aria-hidden="true" />
          <span>Animal Aid and Rescue</span>
        </div>
        <p className="site-footer__text">Cứu hộ, chăm sóc và tìm mái nhà mới cho chó mèo bị bỏ rơi.</p>
        <nav className="site-footer__links" aria-label="Liên kết cuối trang">
          <Link href="/">Thú chờ nhận nuôi</Link>
          <Link href="/rescue">Báo cứu hộ</Link>
          <Link href="/rescue/lookup">Tra cứu báo cáo</Link>
          <Link href="/login">Đăng nhập</Link>
        </nav>
      </div>
    </footer>
  );
}
