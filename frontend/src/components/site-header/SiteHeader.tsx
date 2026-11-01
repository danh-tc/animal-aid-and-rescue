import Link from "next/link";
import "./site-header.scss";

export function SiteHeader() {
  return (
    <header className="site-header">
      <div className="container site-header__inner">
        <Link href="/" className="site-header__logo">
          <span className="site-header__logo-mark" aria-hidden="true">
            🐾
          </span>
          <span className="site-header__logo-text site-header__logo-text--full">Animal Aid and Rescue</span>
          <span className="site-header__logo-text site-header__logo-text--short">AAR</span>
        </Link>
        <nav className="site-header__nav" aria-label="Điều hướng chính">
          <Link href="/" className="site-header__link">
            Nhận nuôi
          </Link>
          <Link href="/login" className="site-header__link">
            Đăng nhập
          </Link>
        </nav>
      </div>
    </header>
  );
}
