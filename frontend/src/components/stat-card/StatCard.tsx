import type { LucideIcon } from "lucide-react";
import { ArrowRight } from "lucide-react";
import Link from "next/link";
import "./stat-card.scss";

interface StatCardProps {
  label: string;
  value: number | string | null;
  href: string;
  icon: LucideIcon;
  tone?: "primary" | "accent" | "warning" | "info";
  hint?: string;
}

export function StatCard({ label, value, href, icon: Icon, tone = "primary", hint }: StatCardProps) {
  return (
    <Link href={href} className={`stat-card stat-card--${tone}`}>
      <span className="stat-card__icon" aria-hidden="true">
        <Icon />
      </span>
      <span className="stat-card__value">{value ?? "–"}</span>
      <span className="stat-card__label">{label}</span>
      {hint && <span className="stat-card__hint">{hint}</span>}
      <ArrowRight className="stat-card__arrow" aria-hidden="true" />
    </Link>
  );
}
