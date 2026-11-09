import { CheckCircle2, CircleDashed } from "lucide-react";
import type { ChecklistItem } from "@/types/api";
import "./checklist.scss";

export function Checklist({ items }: { items: ChecklistItem[] }) {
  return (
    <ul className="checklist">
      {items.map((item) => (
        <li key={item.key} className={`checklist__item checklist__item--${item.passed ? "passed" : "missing"}`}>
          {item.passed ? (
            <CheckCircle2 className="checklist__icon" aria-hidden="true" />
          ) : (
            <CircleDashed className="checklist__icon" aria-hidden="true" />
          )}
          <div className="checklist__text">
            <span className="checklist__label">
              {item.label}
              <span className="sr-only">{item.passed ? ": đạt" : ": chưa đạt"}</span>
            </span>
            {item.detail && <span className="checklist__detail">{item.detail}</span>}
          </div>
          <span className="checklist__state" aria-hidden="true">
            {item.passed ? "Đạt" : "Chưa đạt"}
          </span>
        </li>
      ))}
    </ul>
  );
}
