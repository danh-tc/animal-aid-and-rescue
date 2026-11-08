import type { ReactNode } from "react";
import "./panel.scss";

interface PanelProps {
  title?: ReactNode;
  description?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;

  flush?: boolean;
  as?: "section" | "div" | "article";
}

export function Panel({ title, description, actions, children, flush, as: Tag = "section" }: PanelProps) {
  return (
    <Tag className={`panel${flush ? " panel--flush" : ""}`}>
      {(title || actions) && (
        <header className="panel__header">
          <div className="panel__heading">
            {title && <h2 className="panel__title">{title}</h2>}
            {description && <p className="panel__description">{description}</p>}
          </div>
          {actions && <div className="panel__actions">{actions}</div>}
        </header>
      )}
      <div className="panel__body">{children}</div>
    </Tag>
  );
}
