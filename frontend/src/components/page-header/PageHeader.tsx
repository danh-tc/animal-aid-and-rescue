import { ChevronLeft } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import "./page-header.scss";

interface PageHeaderProps {
  title: ReactNode;
  description?: ReactNode;

  back?: { href: string; label: string };
  meta?: ReactNode;
  actions?: ReactNode;
}

export function PageHeader({ title, description, back, meta, actions }: PageHeaderProps) {
  return (
    <header className="page-header">
      {back && (
        <Link href={back.href} className="page-header__back">
          <ChevronLeft aria-hidden="true" />
          {back.label}
        </Link>
      )}
      <div className="page-header__row">
        <div className="page-header__heading">
          <h1 className="page-header__title">{title}</h1>
          {meta && <div className="page-header__meta">{meta}</div>}
          {description && <p className="page-header__description">{description}</p>}
        </div>
        {actions && <div className="page-header__actions">{actions}</div>}
      </div>
    </header>
  );
}
