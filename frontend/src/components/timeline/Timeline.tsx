import type { ReactNode } from "react";
import "./timeline.scss";

export interface TimelineEntry {
  key: string | number;
  title: ReactNode;
  time: string;
  body?: ReactNode;
}

export function Timeline({ entries }: { entries: TimelineEntry[] }) {
  return (
    <ol className="timeline">
      {entries.map((e) => (
        <li key={e.key} className="timeline__item">
          <span className="timeline__marker" aria-hidden="true" />
          <div className="timeline__content">
            <div className="timeline__head">
              <span className="timeline__title">{e.title}</span>
              <time className="timeline__time">{e.time}</time>
            </div>
            {e.body && <div className="timeline__body">{e.body}</div>}
          </div>
        </li>
      ))}
    </ol>
  );
}
