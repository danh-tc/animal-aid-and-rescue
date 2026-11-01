"use client";

import {
  Ambulance,
  BarChart3,
  ClipboardCheck,
  FileText,
  Globe,
  HeartHandshake,
  Hospital,
  LayoutDashboard,
  LogOut,
  Menu,
  PawPrint,
  Settings2,
  Stethoscope,
  Users,
  X,
} from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { type ReactNode, useEffect, useState } from "react";
import { useAuth } from "@/lib/auth-context";
import { ROLE } from "@/lib/labels";
import "./app-shell.scss";

interface NavItem {
  href: string;
  label: string;
  icon: typeof PawPrint;
  exact?: boolean;
}

interface NavGroup {
  title: string;
  items: NavItem[];
  managerOnly?: boolean;
}

const NAV: NavGroup[] = [
  { title: "Tổng quan", items: [{ href: "/staff", label: "Bảng công việc", icon: LayoutDashboard, exact: true }] },
  {
    title: "Cứu hộ",
    items: [
      { href: "/staff/reports", label: "Báo cáo cứu hộ", icon: FileText },
      { href: "/staff/cases", label: "Ca cứu hộ", icon: Ambulance },
    ],
  },
  {
    title: "Chăm sóc",
    items: [
      { href: "/staff/pets", label: "Hồ sơ thú", icon: PawPrint },
      { href: "/staff/vet-requests", label: "Yêu cầu thú y", icon: Stethoscope },
    ],
  },
  {
    title: "Nhận nuôi",
    items: [
      { href: "/staff/applications", label: "Đơn nhận nuôi", icon: HeartHandshake },
      { href: "/staff/follow-ups", label: "Hậu kiểm", icon: ClipboardCheck },
    ],
  },
  {
    title: "Quản trị",
    managerOnly: true,
    items: [
      { href: "/admin/reports", label: "Báo cáo thống kê", icon: BarChart3 },
      { href: "/admin/users", label: "Tài khoản", icon: Users },
      { href: "/admin/clinics", label: "Cơ sở thú y", icon: Hospital },
      { href: "/admin/parameters", label: "Tham số hệ thống", icon: Settings2 },
    ],
  },
];

export function AppShell({ children }: { children: ReactNode }) {
  const { user, signOut } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);

  useEffect(() => setOpen(false), [pathname]);

  // Esc đóng ngăn kéo trên điện thoại
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  const isActive = (item: NavItem) => (item.exact ? pathname === item.href : pathname.startsWith(item.href));

  function handleLogout() {
    signOut();
    router.push("/login");
  }

  return (
    <div className={`app-shell${open ? " app-shell--open" : ""}`}>
      <header className="app-shell__topbar">
        <button
          type="button"
          className="app-shell__menu-button"
          aria-expanded={open}
          aria-controls="app-sidebar"
          aria-label="Mở menu"
          onClick={() => setOpen(true)}
        >
          <Menu aria-hidden="true" />
        </button>
        <Link href="/staff" className="app-shell__brand">
          <span className="app-shell__brand-mark" aria-hidden="true">
            <PawPrint />
          </span>
          AAR
        </Link>
      </header>

      <div className="app-shell__scrim" aria-hidden="true" onClick={() => setOpen(false)} />

      <aside id="app-sidebar" className="app-shell__sidebar" aria-label="Điều hướng khu làm việc">
        <div className="app-shell__sidebar-head">
          <Link href="/staff" className="app-shell__brand">
            <span className="app-shell__brand-mark" aria-hidden="true">
              <PawPrint />
            </span>
            Animal Aid and Rescue
          </Link>
          <button
            type="button"
            className="app-shell__close"
            aria-label="Đóng menu"
            onClick={() => setOpen(false)}
          >
            <X aria-hidden="true" />
          </button>
        </div>

        <nav className="app-shell__nav">
          {NAV.filter((g) => !g.managerOnly || user?.role === "MANAGER").map((group) => (
            <div key={group.title} className="app-shell__group">
              <p className="app-shell__group-title">{group.title}</p>
              <ul className="app-shell__list">
                {group.items.map((item) => {
                  const active = isActive(item);
                  const Icon = item.icon;
                  return (
                    <li key={item.href}>
                      <Link
                        href={item.href}
                        className={`app-shell__link${active ? " app-shell__link--active" : ""}`}
                        aria-current={active ? "page" : undefined}
                      >
                        <Icon aria-hidden="true" />
                        {item.label}
                      </Link>
                    </li>
                  );
                })}
              </ul>
            </div>
          ))}
        </nav>

        <div className="app-shell__footer">
          <Link href="/" className="app-shell__link">
            <Globe aria-hidden="true" />
            Trang công khai
          </Link>
          {user && (
            <div className="app-shell__user">
              <span className="app-shell__avatar" aria-hidden="true">
                {user.fullName.trim().split(/\s+/).pop()?.charAt(0).toUpperCase()}
              </span>
              <span className="app-shell__user-text">
                <span className="app-shell__user-name">{user.fullName}</span>
                <span className="app-shell__user-role">{ROLE[user.role]}</span>
              </span>
              <button type="button" className="app-shell__logout" onClick={handleLogout} aria-label="Đăng xuất">
                <LogOut aria-hidden="true" />
              </button>
            </div>
          )}
        </div>
      </aside>

      <main id="main" className="app-shell__main" tabIndex={-1}>
        {children}
      </main>
    </div>
  );
}
