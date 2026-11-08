"use client";

import { ClipboardList, Heart, LayoutDashboard, LogOut, Menu, PawPrint, Siren, X } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { isStaff, useAuth } from "@/lib/auth-context";
import "./site-header.scss";

interface NavLink {
  href: string;
  label: string;
  icon: typeof Heart;
}

export function SiteHeader() {
  const { user, ready, signOut } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);

  useEffect(() => setOpen(false), [pathname]);

  const links: NavLink[] = [{ href: "/", label: "Nhận nuôi", icon: Heart }];
  if (user?.role === "ADOPTER") {
    links.push({ href: "/me/applications", label: "Đơn của tôi", icon: ClipboardList });
    links.push({ href: "/me/adoptions", label: "Thú đã nhận", icon: PawPrint });
  }
  if (isStaff(user?.role)) links.push({ href: "/staff", label: "Khu làm việc", icon: LayoutDashboard });

  function isActive(href: string) {
    return href === "/" ? pathname === "/" || pathname.startsWith("/pets") : pathname.startsWith(href);
  }

  function handleLogout() {
    signOut();
    router.push("/");
  }

  return (
    <header className={`site-header${open ? " site-header--open" : ""}`}>
      <div className="container site-header__inner">
        <Link href="/" className="site-header__logo">
          <span className="site-header__logo-mark" aria-hidden="true">
            <PawPrint />
          </span>
          <span className="site-header__logo-text">Animal Aid and Rescue</span>
        </Link>

        <button
          type="button"
          className="site-header__toggle"
          aria-expanded={open}
          aria-controls="site-nav"
          aria-label={open ? "Đóng menu" : "Mở menu"}
          onClick={() => setOpen((v) => !v)}
        >
          {open ? <X aria-hidden="true" /> : <Menu aria-hidden="true" />}
        </button>

        <nav id="site-nav" className="site-header__nav" aria-label="Điều hướng chính">
          <ul className="site-header__links">
            {links.map(({ href, label, icon: Icon }) => (
              <li key={href}>
                <Link
                  href={href}
                  className={`site-header__link${isActive(href) ? " site-header__link--active" : ""}`}
                  aria-current={isActive(href) ? "page" : undefined}
                >
                  <Icon aria-hidden="true" />
                  {label}
                </Link>
              </li>
            ))}
          </ul>

          <div className="site-header__actions">
            <Link href="/rescue" className="site-header__cta">
              <Siren aria-hidden="true" />
              Báo cứu hộ
            </Link>
            {ready && !user && (
              <>
                <Link href="/login" className="site-header__link">
                  Đăng nhập
                </Link>
                <Link href="/register" className="site-header__link site-header__link--outlined">
                  Đăng ký
                </Link>
              </>
            )}
            {ready && user && (
              <div className="site-header__user">
                <span className="site-header__user-name" title={user.email}>
                  {user.fullName}
                </span>
                <button type="button" className="site-header__logout" onClick={handleLogout}>
                  <LogOut aria-hidden="true" />
                  <span>Đăng xuất</span>
                </button>
              </div>
            )}
          </div>
        </nav>
      </div>
    </header>
  );
}
