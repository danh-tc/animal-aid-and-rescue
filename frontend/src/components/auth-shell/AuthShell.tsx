import { CheckCircle2, PawPrint } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import "./auth-shell.scss";

interface AuthShellProps {
  title: string;
  description?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
}

const BENEFITS = [
  "Nộp đơn nhận nuôi chỉ trong vài phút",
  "Theo dõi trạng thái đơn và lịch hậu kiểm",
  "Một tài khoản cho mọi lần nhận nuôi",
];

export function AuthShell({ title, description, children, footer }: AuthShellProps) {
  return (
    <div className="auth-shell">
      <aside className="auth-shell__brand" aria-label="Giới thiệu">
        <Link href="/" className="auth-shell__logo">
          <span className="auth-shell__logo-mark" aria-hidden="true">
            <PawPrint />
          </span>
          Animal Aid and Rescue
        </Link>
        <div className="auth-shell__pitch">
          <p className="auth-shell__headline">Cùng chúng tôi tìm cho các bé một mái nhà.</p>
          <ul className="auth-shell__benefits">
            {BENEFITS.map((b) => (
              <li key={b}>
                <CheckCircle2 aria-hidden="true" />
                {b}
              </li>
            ))}
          </ul>
        </div>
        <p className="auth-shell__note">Tài khoản nhân viên do Quản lý trung tâm cấp.</p>
      </aside>

      <section className="auth-shell__main">
        <Link href="/" className="auth-shell__mobile-logo">
          <span className="auth-shell__logo-mark" aria-hidden="true">
            <PawPrint />
          </span>
          Animal Aid and Rescue
        </Link>
        <div className="auth-shell__panel">
          <header className="auth-shell__header">
            <h1 className="auth-shell__title">{title}</h1>
            {description && <div className="auth-shell__description">{description}</div>}
          </header>
          {children}
          {footer && <footer className="auth-shell__footer">{footer}</footer>}
        </div>
        <Link href="/" className="auth-shell__back">
          ← Về trang chủ
        </Link>
      </section>
    </div>
  );
}
