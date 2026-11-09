import { AlertTriangle, CheckCircle2, Info, XCircle } from "lucide-react";
import type { ReactNode } from "react";
import "./notice.scss";

type Tone = "info" | "success" | "warning" | "error";

const ICONS = { info: Info, success: CheckCircle2, warning: AlertTriangle, error: XCircle } as const;

interface NoticeProps {
  tone?: Tone;
  title?: string;
  children?: ReactNode;
  action?: ReactNode;
}

export function Notice({ tone = "info", title, children, action }: NoticeProps) {
  const Icon = ICONS[tone];
  return (
    <div className={`notice notice--${tone}`} role={tone === "error" ? "alert" : "status"}>
      <Icon className="notice__icon" aria-hidden="true" />
      <div className="notice__body">
        {title && <p className="notice__title">{title}</p>}
        {children && <div className="notice__text">{children}</div>}
      </div>
      {action && <div className="notice__action">{action}</div>}
    </div>
  );
}
