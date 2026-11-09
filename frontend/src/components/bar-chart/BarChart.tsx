"use client";

import { Table2 } from "lucide-react";
import { type CSSProperties, useEffect, useId, useMemo, useRef, useState } from "react";
import "./bar-chart.scss";

export interface BarSeries {
  key: string;
  label: string;
}

interface BarChartProps {
  title: string;

  summary: string;
  categories: string[];
  series: BarSeries[];

  values: number[][];
  format: (v: number) => string;

  formatAxis?: (v: number) => string;
  categoryLabel?: (c: string) => string;
}

const HEIGHT = 220;
const PAD = { top: 12, right: 8, bottom: 28, left: 56 };
const GAP = 2;

function niceMax(max: number): { top: number; step: number } {
  if (max <= 0) return { top: 4, step: 1 };
  const raw = max / 4;
  const pow = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 5, 10].map((m) => m * pow).find((s) => s >= raw) ?? raw;
  return { top: step * 4, step };
}

function tooltipPosition(x: number, width: number): CSSProperties {
  if (x > width * 0.7) return { right: `${width - x + 8}px` };
  if (x < width * 0.3) return { left: `${x + 8}px` };
  return { left: `${x}px`, transform: "translateX(-50%)" };
}

export function BarChart({ title, summary, categories, series, values, format, formatAxis, categoryLabel }: BarChartProps) {
  const id = useId();
  const [active, setActive] = useState<number | null>(null);
  const [showTable, setShowTable] = useState(false);
  const plotRef = useRef<HTMLDivElement>(null);
  const [measured, setMeasured] = useState(0);

  useEffect(() => {
    const el = plotRef.current;
    if (!el) return;
    const observer = new ResizeObserver(([entry]) => setMeasured(Math.floor(entry.contentRect.width)));
    observer.observe(el);
    return () => observer.disconnect();
  }, [showTable]);

  useEffect(() => {
    const el = plotRef.current;
    if (el) el.scrollLeft = el.scrollWidth;
  }, [measured, categories.length, showTable]);
  const label = categoryLabel ?? ((c: string) => c);
  const axis = formatAxis ?? format;

  const { top, step } = useMemo(() => niceMax(Math.max(0, ...values.flat())), [values]);
  const minWidth = categories.length * (series.length * 6 + 34) + PAD.left + PAD.right;
  const width = Math.max(minWidth, measured || 640);
  const plotW = width - PAD.left - PAD.right;
  const plotH = HEIGHT - PAD.top - PAD.bottom;
  const band = plotW / Math.max(1, categories.length);
  const barW = Math.max(4, Math.min(18, (band - 12) / series.length - GAP));
  const groupW = series.length * barW + (series.length - 1) * GAP;
  const y = (v: number) => PAD.top + plotH - (v / top) * plotH;
  const ticks = [0, 1, 2, 3, 4].map((i) => i * step);

  return (
    <figure className="bar-chart" aria-labelledby={`${id}-title`}>
      <figcaption className="bar-chart__head">
        <span id={`${id}-title`} className="bar-chart__title">
          {title}
        </span>
        {series.length > 1 && (
          <ul className="bar-chart__legend">
            {series.map((s, i) => (
              <li key={s.key} className="bar-chart__legend-item">
                <span className={`bar-chart__swatch bar-chart__swatch--${i + 1}`} aria-hidden="true" />
                {s.label}
              </li>
            ))}
          </ul>
        )}
        <button
          type="button"
          className="bar-chart__toggle"
          aria-pressed={showTable}
          onClick={() => setShowTable((v) => !v)}
        >
          <Table2 aria-hidden="true" />
          {showTable ? "Xem biểu đồ" : "Xem dạng bảng"}
        </button>
      </figcaption>

      {showTable ? (
        <div className="bar-chart__table-wrap">
          <table className="bar-chart__table">
            <caption className="sr-only">{title}</caption>
            <thead>
              <tr>
                <th scope="col">Kỳ</th>
                {series.map((s) => (
                  <th key={s.key} scope="col">
                    {s.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {categories.map((c, i) => (
                <tr key={c}>
                  <th scope="row">{label(c)}</th>
                  {series.map((s, j) => (
                    <td key={s.key}>{format(values[i][j])}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="bar-chart__plot" ref={plotRef}>
          <svg
            viewBox={`0 0 ${width} ${HEIGHT}`}
            width={width}
            height={HEIGHT}
            className="bar-chart__svg"
            role="img"
            aria-label={summary}
          >
            {ticks.map((t) => (
              <g key={t}>
                <line className="bar-chart__grid" x1={PAD.left} x2={width - PAD.right} y1={y(t)} y2={y(t)} />
                <text className="bar-chart__axis" x={PAD.left - 8} y={y(t)} dy="0.32em" textAnchor="end">
                  {axis(t)}
                </text>
              </g>
            ))}
            {categories.map((c, i) => {
              const x0 = PAD.left + i * band + (band - groupW) / 2;
              return (
                <g
                  key={c}
                  className={`bar-chart__group${active === i ? " bar-chart__group--active" : ""}`}
                  onMouseEnter={() => setActive(i)}
                  onMouseLeave={() => setActive(null)}
                  onFocus={() => setActive(i)}
                  onBlur={() => setActive(null)}
                  tabIndex={0}
                  aria-label={`${label(c)}: ${series.map((s, j) => `${s.label} ${format(values[i][j])}`).join(", ")}`}
                >
                  {}
                  <rect className="bar-chart__hit" x={PAD.left + i * band} y={PAD.top} width={band} height={plotH} />
                  {series.map((s, j) => {
                    const v = values[i][j];
                    const h = Math.max(0, y(0) - y(v));
                    const r = Math.min(4, h / 2, barW / 2);
                    const x = x0 + j * (barW + GAP);
                    const yy = y(v);

                    const d = h <= 0 ? "" : `M${x},${y(0)} V${yy + r} Q${x},${yy} ${x + r},${yy} H${x + barW - r} Q${x + barW},${yy} ${x + barW},${yy + r} V${y(0)} Z`;
                    return d ? <path key={s.key} d={d} className={`bar-chart__bar bar-chart__bar--${j + 1}`} /> : null;
                  })}
                  <text className="bar-chart__axis" x={PAD.left + i * band + band / 2} y={HEIGHT - 8} textAnchor="middle">
                    {label(c)}
                  </text>
                </g>
              );
            })}
          </svg>
          {active !== null && (
            <div
              className="bar-chart__tooltip"
              style={tooltipPosition(PAD.left + active * band + band / 2, width)}
              role="status"
            >
              <p className="bar-chart__tooltip-title">{label(categories[active])}</p>
              {series.map((s, j) => (
                <p key={s.key} className="bar-chart__tooltip-row">
                  <span className={`bar-chart__swatch bar-chart__swatch--${j + 1}`} aria-hidden="true" />
                  <span>{s.label}</span>
                  <strong>{format(values[active][j])}</strong>
                </p>
              ))}
            </div>
          )}
        </div>
      )}
    </figure>
  );
}
