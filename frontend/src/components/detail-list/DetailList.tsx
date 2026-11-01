import type { ReactNode } from "react";
import "./detail-list.scss";

export interface DetailItem {
  label: string;
  value: ReactNode;
  /** Chiếm cả hàng, dùng cho mô tả dài. */
  wide?: boolean;
}

export function DetailList({ items }: { items: DetailItem[] }) {
  return (
    <dl className="detail-list">
      {items.map((item) => (
        <div key={item.label} className={`detail-list__item${item.wide ? " detail-list__item--wide" : ""}`}>
          <dt className="detail-list__term">{item.label}</dt>
          <dd className="detail-list__value">{item.value ?? "—"}</dd>
        </div>
      ))}
    </dl>
  );
}
