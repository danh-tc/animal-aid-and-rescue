"use client";

import { type KeyboardEvent, useRef } from "react";
import "./tabs.scss";

export interface TabItem {
  id: string;
  label: string;
  count?: number;
}

interface TabsProps {
  label: string;
  items: TabItem[];
  value: string;
  onChange: (id: string) => void;
}

/** Thanh tab theo mẫu WAI-ARIA: phím mũi tên chuyển tab, panel dùng id `tab-panel-{id}`. */
export function Tabs({ label, items, value, onChange }: TabsProps) {
  const refs = useRef<Record<string, HTMLButtonElement | null>>({});

  function handleKey(event: KeyboardEvent<HTMLButtonElement>, index: number) {
    let next = -1;
    if (event.key === "ArrowRight") next = (index + 1) % items.length;
    if (event.key === "ArrowLeft") next = (index - 1 + items.length) % items.length;
    if (event.key === "Home") next = 0;
    if (event.key === "End") next = items.length - 1;
    if (next < 0) return;
    event.preventDefault();
    const id = items[next].id;
    onChange(id);
    refs.current[id]?.focus();
  }

  return (
    <div className="tabs" role="tablist" aria-label={label}>
      {items.map((item, i) => {
        const selected = item.id === value;
        return (
          <button
            key={item.id}
            ref={(el) => {
              refs.current[item.id] = el;
            }}
            type="button"
            role="tab"
            id={`tab-${item.id}`}
            aria-selected={selected}
            aria-controls={`tab-panel-${item.id}`}
            tabIndex={selected ? 0 : -1}
            className={`tabs__tab${selected ? " tabs__tab--active" : ""}`}
            onClick={() => onChange(item.id)}
            onKeyDown={(e) => handleKey(e, i)}
          >
            {item.label}
            {item.count !== undefined && <span className="tabs__count">{item.count}</span>}
          </button>
        );
      })}
    </div>
  );
}

export function TabPanel({ id, children }: { id: string; children: React.ReactNode }) {
  return (
    <div role="tabpanel" id={`tab-panel-${id}`} aria-labelledby={`tab-${id}`} className="tabs__panel" tabIndex={0}>
      {children}
    </div>
  );
}
