import type { ReactNode } from "react";
import type { Labeled, Tone } from "@/lib/labels";
import "./badge.scss";

interface BadgeProps {
  tone?: Tone;
  children: ReactNode;
  /** Thêm chấm tròn trước chữ, chữ vẫn mang nghĩa nên không phụ thuộc màu. */
  dot?: boolean;
}

export function Badge({ tone = "neutral", children, dot = true }: BadgeProps) {
  return (
    <span className={`badge badge--${tone}`}>
      {dot && <span className="badge__dot" aria-hidden="true" />}
      {children}
    </span>
  );
}

/** Badge cho giá trị enum đã có nhãn và sắc thái trong lib/labels. */
export function StatusBadge<K extends string>({ map, value }: { map: Record<K, Labeled>; value: K | null | undefined }) {
  if (!value) return null;
  const item = map[value] ?? { label: value, tone: "neutral" as Tone };
  return <Badge tone={item.tone}>{item.label}</Badge>;
}
