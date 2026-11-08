"use client";

import "./segmented.scss";

export interface SegmentOption<V extends string> {
  value: V;
  label: string;
}

interface SegmentedProps<V extends string> {
  label: string;
  options: SegmentOption<V>[];
  value: V;
  onChange: (value: V) => void;
}

export function Segmented<V extends string>({ label, options, value, onChange }: SegmentedProps<V>) {
  return (
    <div className="segmented" role="group" aria-label={label}>
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          className={`segmented__option${o.value === value ? " segmented__option--active" : ""}`}
          aria-pressed={o.value === value}
          onClick={() => onChange(o.value)}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}
