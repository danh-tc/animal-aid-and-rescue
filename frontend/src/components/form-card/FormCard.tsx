import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import "./form-card.scss";

interface FormCardProps {
  title: string;
  description?: ReactNode;
  icon?: LucideIcon;
  children: ReactNode;
  footer?: ReactNode;
  wide?: boolean;
}

export function FormCard({ title, description, icon: Icon, children, footer, wide }: FormCardProps) {
  return (
    <div className={`form-card${wide ? " form-card--wide" : ""}`}>
      <header className="form-card__header">
        {Icon && (
          <span className="form-card__icon" aria-hidden="true">
            <Icon />
          </span>
        )}
        <h1 className="form-card__title">{title}</h1>
        {description && <div className="form-card__description">{description}</div>}
      </header>
      <div className="form-card__body">{children}</div>
      {footer && <footer className="form-card__footer">{footer}</footer>}
    </div>
  );
}
